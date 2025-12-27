package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;

/**
 * Puerto de entrada para generar embeddings de intents
 */
public interface GenerateIntentEmbeddingsUseCase {
    /**
     * Genera embeddings para todos los intents que no los tienen
     *
     * @return Número de intents procesados
     */
    Uni<Integer> generateMissingEmbeddings();

    /**
     * Regenera embeddings para todos los intents (sobrescribe los existentes)
     *
     * @return Número de intents procesados
     */
    Uni<Integer> regenerateAllEmbeddings();

    /**
     * Genera embedding para un intent específico
     *
     * @param intentName Nombre del intent
     * @return true si se generó correctamente
     */
    Uni<Boolean> generateEmbeddingForIntent(String intentName);
}
