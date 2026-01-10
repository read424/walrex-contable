package org.walrex.application.port.output;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.AccountingBookType;
import org.walrex.domain.model.CachedEmbedding;

public interface EmbeddingCachePort {

    /**
     * Obtiene el embedding cacheado.
     *
     * @param imageHash Hash SHA-256 de la imagen
     * @param bookType Tipo de libro contable
     * @return Uni con CachedEmbedding si existe, null si no existe
     */
    Uni<CachedEmbedding> get(String imageHash, AccountingBookType bookType);

    /**
     * Guarda el embedding en cache.
     *
     * @param imageHash Hash SHA-256 de la imagen
     * @param bookType Tipo de libro contable
     * @param cachedEmbedding Embedding y chunk text
     * @return Uni que se completa cuando se almacena
     */
    Uni<Void> put(String imageHash, AccountingBookType bookType, CachedEmbedding cachedEmbedding);

    /**
     * Invalida el cache de un embedding específico.
     */
    Uni<Void> invalidate(String imageHash, AccountingBookType bookType);
}
