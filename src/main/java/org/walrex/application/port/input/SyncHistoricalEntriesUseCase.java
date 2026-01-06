package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;

/**
 * Caso de uso para sincronizar asientos contables históricos a Qdrant.
 *
 * Los asientos históricos se usan en RAG para:
 * - Encontrar patrones contables similares
 * - Aprender de registros previos
 * - Mejorar las sugerencias del LLM
 */
public interface SyncHistoricalEntriesUseCase {

    /**
     * Sincroniza un asiento específico a Qdrant.
     *
     * @param journalEntryId ID del asiento a sincronizar
     * @return Uni<Void> cuando se complete la sincronización
     */
    Uni<Void> syncEntry(Integer journalEntryId);

    /**
     * Remueve un asiento de Qdrant.
     *
     * @param journalEntryId ID del asiento a remover
     * @return Uni<Void> cuando se complete la eliminación
     */
    Uni<Void> removeEntry(Integer journalEntryId);

    /**
     * Sincroniza todos los asientos existentes a Qdrant.
     * Útil para migración inicial.
     *
     * @return Uni<Integer> con el número de asientos sincronizados
     */
    Uni<Integer> syncAllEntries();
}
