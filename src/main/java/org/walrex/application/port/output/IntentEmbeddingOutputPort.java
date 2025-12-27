package org.walrex.application.port.output;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.IntentEmbeddingEntity;

/**
 * Puerto de salida para persistencia de embeddings de intents
 * Siguiendo el principio de Inversión de Dependencias (DIP)
 */
public interface IntentEmbeddingOutputPort {

    /**
     * Encuentra todos los intents activos que no tienen embedding generado
     *
     * @return Multi de intents sin embedding
     */
    Multi<IntentEmbeddingEntity> findIntentsWithoutEmbedding();

    /**
     * Encuentra todos los intents activos
     *
     * @return Multi de intents activos
     */
    Multi<IntentEmbeddingEntity> findAllActiveIntents();

    /**
     * Encuentra un intent por su nombre
     *
     * @param intentName Nombre del intent
     * @return Intent encontrado o vacío
     */
    Uni<IntentEmbeddingEntity> findByIntentName(String intentName);

    /**
     * Persiste un intent (crear o actualizar)
     *
     * @param intent Intent a persistir
     * @return Intent persistido
     */
    Uni<IntentEmbeddingEntity> save(IntentEmbeddingEntity intent);

    /**
     * Cuenta los intents activos
     *
     * @return Número de intents activos
     */
    Uni<Long> countActiveIntents();

    /**
     * Actualiza el embedding de un intent usando query nativa
     * (Workaround para bug de Hibernate Reactive con PGvector)
     *
     * @param intentId ID del intent
     * @param embedding Vector embedding
     * @return Número de filas actualizadas
     */
    Uni<Integer> updateEmbedding(Long intentId, float[] embedding);
}
