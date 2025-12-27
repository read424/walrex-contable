package org.walrex.infrastructure.adapter.outbound.llm;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.data.embedding.Embedding;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.walrex.application.port.output.EmbeddingOutputPort;

import java.util.List;


/**
 * Adaptador para generación de embeddings usando Ollama
 */
@Slf4j
@ApplicationScoped
@RegisterForReflection
public class OllamaEmbeddingAdapter implements EmbeddingOutputPort {

    @Inject
    EmbeddingModel embeddingModel;

    @Override
    public Uni<float[]> generateEmbedding(String text) {
        log.debug("Generating embedding for text: {}", text.substring(0, Math.min(50, text.length())));

        // Execute blocking LangChain4j call on worker thread
        // Mutiny preserves the original context (event-loop) automatically after worker execution
        return Uni.createFrom().item(() -> {
            try {
                Embedding embedding = embeddingModel.embed(text).content();
                List<Float> vector = embedding.vectorAsList();

                // Convert List<Float> to float[]
                float[] result = new float[vector.size()];
                for (int i = 0; i < vector.size(); i++) {
                    result[i] = vector.get(i);
                }

                log.debug("Generated embedding with {} dimensions", result.length);
                return result;
            } catch (Exception e) {
                log.error("Error generating embedding", e);
                throw new RuntimeException("Failed to generate embedding", e);
            }
        })
        // Run blocking operation on worker pool, then Mutiny returns to original context
        .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }
}
