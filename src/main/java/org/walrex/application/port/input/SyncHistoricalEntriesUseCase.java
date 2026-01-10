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
     * Sincroniza un asiento específico a Qdrant con detección automática de caché.
     *
     * 🔑 OPTIMIZACIÓN AUTOMÁTICA:
     * - Busca documentos adjuntos en las líneas del asiento
     * - Si encuentra documentos, genera hash SHA-256 y busca en Redis cache
     * - Si existe en caché, REUTILIZA el embedding (ahorra costos y latencia)
     * - Si no existe, genera nuevo embedding como fallback
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
