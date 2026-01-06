package org.walrex.domain.service;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.walrex.application.port.input.SyncHistoricalEntriesUseCase;
import org.walrex.application.port.output.JournalEntryQueryPort;
import org.walrex.application.port.output.VectorStorePort;
import org.walrex.domain.model.JournalEntry;
import org.walrex.infrastructure.adapter.logging.LogExecutionTime;

/**
 * Servicio para sincronizar asientos contables históricos a Qdrant.
 * Esto permite que el RAG aprenda de asientos previos.
 */
@Slf4j
@ApplicationScoped
public class HistoricalEntriesSyncService implements SyncHistoricalEntriesUseCase {

    @Inject
    JournalEntryQueryPort journalEntryQueryPort;

    @Inject
    JournalEntryChunkingService chunkingService;

    @Inject
    VectorStorePort vectorStorePort;

    @ConfigProperty(name = "rag.historical.auto-sync-enabled", defaultValue = "true")
    Boolean autoSyncEnabled;

    @Override
    @WithSpan("HistoricalEntriesSyncService.syncEntry")
    @LogExecutionTime(value = LogExecutionTime.LogLevel.INFO, logParameters = true)
    public Uni<Void> syncEntry(Integer journalEntryId) {
        if (!autoSyncEnabled) {
            log.debug("Historical entries auto-sync is disabled, skipping entry {}", journalEntryId);
            return Uni.createFrom().voidItem();
        }

        log.info("Syncing journal entry {} to Qdrant", journalEntryId);

        return journalEntryQueryPort.findById(journalEntryId)
                .onItem().transformToUni(optionalEntry -> {
                    if (optionalEntry.isEmpty()) {
                        return Uni.createFrom().failure(
                                new IllegalArgumentException("Journal entry not found: " + journalEntryId)
                        );
                    }

                    JournalEntry entry = optionalEntry.get();

                    // Crear chunk con embedding
                    return chunkingService.createHistoricalChunk(entry)
                            .chain(chunk -> vectorStorePort.upsertHistoricalEntryChunk(chunk));
                })
                .onItem().invoke(() ->
                        log.info("Successfully synced journal entry {} to Qdrant", journalEntryId)
                )
                .onFailure().invoke(throwable ->
                        log.error("Failed to sync journal entry {} to Qdrant", journalEntryId, throwable)
                );
    }

    @Override
    @WithSpan("HistoricalEntriesSyncService.removeEntry")
    @LogExecutionTime(value = LogExecutionTime.LogLevel.INFO, logParameters = true)
    public Uni<Void> removeEntry(Integer journalEntryId) {
        log.info("Removing journal entry {} from Qdrant", journalEntryId);
        return vectorStorePort.deleteHistoricalEntryChunk(journalEntryId)
                .onItem().invoke(() -> log.info("Successfully removed journal entry {} from Qdrant", journalEntryId))
                .onFailure().invoke(throwable -> log.error("Failed to remove journal entry {} from Qdrant", journalEntryId, throwable));
    }

    @Override
    @WithSpan("HistoricalEntriesSyncService.syncAllEntries")
    @LogExecutionTime(value = LogExecutionTime.LogLevel.INFO, logParameters = true)
    public Uni<Integer> syncAllEntries() {
        log.info("Starting sync of ALL historical journal entries to Qdrant");

        // Buscar todos los asientos (sin filtro)
        return journalEntryQueryPort.findAllWithFilter(null)
                .onItem().transformToUni(entries -> {
                    log.info("Found {} journal entries to sync", entries.size());

                    if (entries.isEmpty()) {
                        return Uni.createFrom().item(0);
                    }

                    // Sincronizar todos en paralelo
                    return Uni.combine().all().unis(
                            entries.stream()
                                    .map(entry -> syncSingleEntry(entry))
                                    .toList()
                    ).combinedWith(results -> results.size());
                })
                .onItem().invoke(count ->
                        log.info("Successfully synced {} historical journal entries to Qdrant", count)
                )
                .onFailure().invoke(throwable ->
                        log.error("Failed to sync all historical entries", throwable)
                );
    }

    /**
     * Sincroniza un asiento individual (helper method).
     */
    private Uni<Void> syncSingleEntry(JournalEntry entry) {
        return chunkingService.createHistoricalChunk(entry)
                .chain(chunk -> vectorStorePort.upsertHistoricalEntryChunk(chunk))
                .onItem().invoke(() ->
                        log.debug("Synced entry ID {} to Qdrant", entry.getId())
                )
                .onFailure().recoverWithItem(throwable -> {
                    log.warn("Failed to sync entry ID {}, continuing...", entry.getId(), throwable);
                    return null; // Continuar con los demás aunque uno falle
                });
    }
}
