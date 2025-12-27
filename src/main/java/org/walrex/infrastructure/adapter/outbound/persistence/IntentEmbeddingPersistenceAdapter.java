package org.walrex.infrastructure.adapter.outbound.persistence;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.walrex.application.port.output.IntentEmbeddingOutputPort;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.IntentEmbeddingEntity;
import org.walrex.infrastructure.adapter.outbound.persistence.repository.IntentEmbeddingRepository;

/**
 * Adaptador de persistencia para embeddings de intents
 * Implementa el puerto de salida siguiendo arquitectura hexagonal
 */
@ApplicationScoped
public class IntentEmbeddingPersistenceAdapter implements IntentEmbeddingOutputPort {

    @Inject
    IntentEmbeddingRepository repository;

    @Override
    public Multi<IntentEmbeddingEntity> findIntentsWithoutEmbedding() {
        return repository.find("embedding is null and enabled = true")
                .list()
                .onItem().transformToMulti(list -> Multi.createFrom().iterable(list));
    }

    @Override
    public Multi<IntentEmbeddingEntity> findAllActiveIntents() {
        return repository.findAllActive();
    }

    @Override
    public Uni<IntentEmbeddingEntity> findByIntentName(String intentName) {
        return repository.findByIntentName(intentName);
    }

    @Override
    public Uni<IntentEmbeddingEntity> save(IntentEmbeddingEntity intent) {
        return repository.persistAndFlush(intent);
    }

    @Override
    public Uni<Long> countActiveIntents() {
        return repository.countActive();
    }

    @Override
    public Uni<Integer> updateEmbedding(Long intentId, float[] embedding) {
        return repository.updateEmbedding(intentId, embedding);
    }
}
