package org.walrex.infrastructure.adapter.outbound.listener;

import jakarta.enterprise.inject.spi.CDI;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import lombok.extern.slf4j.Slf4j;
import org.walrex.application.port.input.SyncHistoricalEntriesUseCase;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.JournalEntryEntity;

/**
 * JPA Entity Listener para JournalEntryEntity.
 * Se ejecuta en respuesta a eventos del ciclo de vida del asiento contable.
 *
 * Sincroniza automáticamente los asientos a Qdrant para que el RAG pueda
 * aprender de registros históricos y mejorar las sugerencias futuras.
 *
 * Compatible con Hibernate Reactive - usa CDI para obtener beans.
 */
@Slf4j
public class JournalEntryEntityListener {

    /**
     * Se ejecuta después de que un nuevo asiento es persistido en la DB.
     * Trigger la sincronización a Qdrant de forma asíncrona.
     */
    @PostPersist
    public void onPostPersist(JournalEntryEntity entry) {
        log.debug("PostPersist callback triggered for journal entry: {}", entry.getId());

        try {
            SyncHistoricalEntriesUseCase syncUseCase = CDI.current()
                    .select(SyncHistoricalEntriesUseCase.class)
                    .get();

            // Sincronizar a Qdrant de forma asíncrona
            syncUseCase.syncEntry(entry.getId())
                    .subscribe().with(
                            success -> log.info("Successfully synced new journal entry {} to Qdrant", entry.getId()),
                            failure -> log.error("Failed to sync new journal entry {} to Qdrant", entry.getId(), failure)
                    );
        } catch (Exception e) {
            log.error("Error in PostPersist callback for journal entry {}", entry.getId(), e);
        }
    }

    /**
     * Se ejecuta después de que un asiento es actualizado en la DB.
     * Re-sincroniza el asiento a Qdrant con los datos actualizados.
     */
    @PostUpdate
    public void onPostUpdate(JournalEntryEntity entry) {
        log.debug("PostUpdate callback triggered for journal entry: {}", entry.getId());

        try {
            SyncHistoricalEntriesUseCase syncUseCase = CDI.current()
                    .select(SyncHistoricalEntriesUseCase.class)
                    .get();

            // Si la entidad se ha eliminado lógicamente (soft delete), la eliminamos de Qdrant.
            // Si no, la re-sincronizamos para mantener el contexto actualizado.
            if (entry.getDeletedAt() != null) {
                log.info("Journal entry {} was soft-deleted. Removing from Qdrant.", entry.getId());
                syncUseCase.removeEntry(entry.getId())
                        .subscribe().with(
                                success -> log.info("Successfully removed soft-deleted journal entry {} from Qdrant", entry.getId()),
                                failure -> log.error("Failed to remove soft-deleted journal entry {} from Qdrant", entry.getId(), failure)
                        );
            } else {
                log.info("Journal entry {} was updated. Re-syncing to Qdrant.", entry.getId());
                syncUseCase.syncEntry(entry.getId())
                        .subscribe().with(
                                success -> log.info("Successfully re-synced updated journal entry {} to Qdrant", entry.getId()),
                                failure -> log.error("Failed to re-sync updated journal entry {} from Qdrant", entry.getId(), failure)
                        );
            }
        } catch (Exception e) {
            log.error("Error in PostUpdate callback for journal entry {}", entry.getId(), e);
        }
    }

    // Nota: La eliminación de Qdrant se maneja en onPostUpdate al detectar una eliminación lógica (soft delete).
}
