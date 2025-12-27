package org.walrex.application.port.output;

import io.smallrye.mutiny.Uni;

/**
 * Puerto de salida para generación de embeddings
 */
public interface EmbeddingOutputPort {
    /**
     * Genera un embedding (vector) a partir de un texto
     *
     * @param text Texto a convertir en embedding
     * @return Uni con el vector de embeddings (array de floats)
     */
    Uni<float[]> generateEmbedding(String text);
}
