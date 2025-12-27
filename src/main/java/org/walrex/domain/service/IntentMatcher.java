package org.walrex.domain.service;

import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.walrex.application.port.output.EmbeddingOutputPort;
import org.walrex.domain.model.Intent;
import org.walrex.infrastructure.adapter.outbound.persistence.repository.IntentEmbeddingRepository;

import java.util.Arrays;

/**
 * Servicio para detectar la intención del usuario usando búsqueda semántica
 */
@Slf4j
@ApplicationScoped
public class IntentMatcher {

    @Inject
    EmbeddingOutputPort embeddingAdapter;

    @Inject
    IntentEmbeddingRepository intentRepository;

    @Inject
    Vertx vertx;

    // Umbral mínimo de similitud (0.0 a 1.0)
    // Valores típicos: 0.5-0.6 para embeddings normalizados
    private static final double SIMILARITY_THRESHOLD = 0.55;

    /**
     * Detecta la intención del usuario a partir de su mensaje
     *
     * @param userMessage Mensaje del usuario
     * @return Intent detectado con score de similitud, o null si no supera el threshold
     */
    public Uni<Intent> detectIntent(String userMessage) {
        log.info("Detecting intent for message: {}", userMessage);

        return embeddingAdapter.generateEmbedding(userMessage)
                // CRÍTICO: Volver al event-loop antes de operación DB
                .emitOn(command -> vertx.getOrCreateContext().runOnContext(v -> command.run()))
                .onItem().transformToUni(embedding -> {
                    log.debug("Generated embedding with {} dimensions", embedding.length);
                    return intentRepository.findMostSimilarWithScore(embedding, SIMILARITY_THRESHOLD);
                })
                .map(intentWithScore -> {
                    if (intentWithScore == null) {
                        log.warn("No intent matched for message (threshold: {})", SIMILARITY_THRESHOLD);
                        return null;
                    }

                    log.info("Intent detected: {} (score: {})",
                            intentWithScore.intent().getIntentName(),
                            intentWithScore.similarityScore());

                    // examplePhrases puede ser null porque no lo cargamos en la query (no lo necesitamos)
                    var examplePhrases = intentWithScore.intent().getExamplePhrases();
                    var examplePhrasesList = examplePhrases != null
                            ? Arrays.asList(examplePhrases)
                            : java.util.List.<String>of();

                    return new Intent(
                            intentWithScore.intent().getIntentName(),
                            intentWithScore.intent().getDescription(),
                            examplePhrasesList,
                            intentWithScore.intent().getToolName(),
                            intentWithScore.intent().getPromptTemplate(),
                            intentWithScore.similarityScore()
                    );
                });
    }

    /**
     * Calcula el score de similitud entre dos textos
     * Útil para testing y debugging
     *
     * @param text1 Primer texto
     * @param text2 Segundo texto
     * @return Score de similitud (0.0 a 1.0)
     */
    public Uni<Double> calculateSimilarity(String text1, String text2) {
        Uni<float[]> embedding1 = embeddingAdapter.generateEmbedding(text1);
        Uni<float[]> embedding2 = embeddingAdapter.generateEmbedding(text2);

        return Uni.combine().all().unis(embedding1, embedding2)
                .asTuple()
                .map(tuple -> cosineSimilarity(tuple.getItem1(), tuple.getItem2()));
    }

    /**
     * Calcula similitud de coseno entre dos vectores
     */
    private double cosineSimilarity(float[] vec1, float[] vec2) {
        if (vec1.length != vec2.length) {
            throw new IllegalArgumentException("Vectors must have same length");
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < vec1.length; i++) {
            dotProduct += vec1[i] * vec2[i];
            norm1 += vec1[i] * vec1[i];
            norm2 += vec2[i] * vec2[i];
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
}
