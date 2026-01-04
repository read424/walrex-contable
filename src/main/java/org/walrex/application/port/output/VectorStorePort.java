package org.walrex.application.port.output;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.AccountChunk;
import org.walrex.domain.model.AccountSearchResult;
import org.walrex.domain.model.HistoricalEntryChunk;
import org.walrex.domain.model.HybridSearchResult;

import java.util.List;

/**
 * Puerto de salida para operaciones con la base de datos vectorial (Qdrant).
 * Este puerto será implementado por un adapter en la capa de infraestructura.
 */
public interface VectorStorePort {

    /**
     * Inserta o actualiza el embedding de una cuenta en el vector store.
     *
     * @param accountChunk Chunk con la información de la cuenta y su embedding
     * @return Uni<Void> que completa cuando la operación finaliza
     */
    Uni<Void> upsertAccountEmbedding(AccountChunk accountChunk);

    /**
     * Elimina el embedding de una cuenta del vector store.
     *
     * @param accountId ID de la cuenta a eliminar
     * @return Uni<Void> que completa cuando la eliminación finaliza
     */
    Uni<Void> deleteAccountEmbedding(Integer accountId);

    /**
     * Busca cuentas similares usando un embedding vectorial.
     *
     * @param queryEmbedding Vector de búsqueda
     * @param limit          Número máximo de resultados
     * @return Uni con lista de resultados ordenados por similitud
     */
    Uni<List<AccountSearchResult>> searchSimilar(float[] queryEmbedding, int limit);

    /**
     * Busca cuentas similares filtrando por metadatos.
     *
     * @param queryEmbedding Vector de búsqueda
     * @param limit          Número máximo de resultados
     * @param filters        Filtros de metadata (ej: tipo de cuenta, activa)
     * @return Uni con lista de resultados filtrados
     */
    Uni<List<AccountSearchResult>> searchSimilarWithFilters(
            float[] queryEmbedding,
            int limit,
            java.util.Map<String, Object> filters
    );

    /**
     * Verifica si la colección de Qdrant existe y está configurada correctamente.
     *
     * @return Uni<Boolean> true si la colección existe
     */
    Uni<Boolean> collectionExists();

    /**
     * Crea la colección en Qdrant con la configuración necesaria.
     *
     * @return Uni<Void> que completa cuando la colección es creada
     */
    Uni<Void> createCollection();

    /**
     * Almacena un chunk de asiento histórico en Qdrant.
     *
     * @param chunk Chunk con la información del asiento histórico y su embedding
     * @return Uni<Void> que completa cuando la operación finaliza
     */
    Uni<Void> upsertHistoricalEntryChunk(HistoricalEntryChunk chunk);

    /**
     * Busca asientos históricos similares por embedding.
     *
     * @param queryEmbedding Vector de búsqueda
     * @param limit          Número máximo de resultados
     * @return Uni con lista de asientos históricos ordenados por similitud
     */
    Uni<List<HistoricalEntryChunk>> searchSimilarHistoricalEntries(float[] queryEmbedding, int limit);

    /**
     * Búsqueda híbrida: retorna tanto cuentas como asientos históricos.
     *
     * @param queryEmbedding Vector de búsqueda
     * @param accountLimit   Número máximo de cuentas a retornar
     * @param historicalLimit Número máximo de asientos históricos a retornar
     * @return Uni con resultado híbrido conteniendo ambos tipos de datos
     */
    Uni<HybridSearchResult> searchHybrid(float[] queryEmbedding, int accountLimit, int historicalLimit);
}
