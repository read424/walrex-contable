package org.walrex.domain.service;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.walrex.application.port.input.GenerateIntentEmbeddingsUseCase;
import org.walrex.application.port.output.EmbeddingOutputPort;
import org.walrex.application.port.output.IntentEmbeddingOutputPort;
import org.walrex.infrastructure.adapter.outbound.logging.EmbeddingDebugLogger;

import java.util.Arrays;
import java.util.List;

/**
 * Servicio de dominio para generar embeddings de intents
 * Sigue arquitectura hexagonal usando puertos de salida
 */
@Slf4j
@ApplicationScoped
public class EmbeddingGeneratorService implements GenerateIntentEmbeddingsUseCase {

    @Inject
    IntentEmbeddingOutputPort intentPersistence;

    @Inject
    EmbeddingOutputPort embeddingAdapter;

    @Inject
    Vertx vertx;

    @Inject
    org.walrex.infrastructure.adapter.outbound.logging.AsyncFileLogger debugLogger;

    /**
     * Genera un embedding vectorial para un texto dado.
     * Método público para uso general en otros servicios.
     *
     * @param text Texto para generar el embedding
     * @return Uni con el array de floats del embedding
     */
    public Uni<float[]> generate(String text) {
        log.debug("Generating embedding for text of length: {}", text != null ? text.length() : 0);

        if (text == null || text.isBlank()) {
            log.warn("Empty text provided, returning empty embedding");
            return Uni.createFrom().failure(new IllegalArgumentException("Text cannot be null or empty"));
        }

        return embeddingAdapter.generateEmbedding(text);
    }

    @Override
    public Uni<Integer> generateMissingEmbeddings() {
        log.info("Starting generation of missing embeddings");

        return Panache.withTransaction(() ->
                intentPersistence.findIntentsWithoutEmbedding()
                        .collect().asList()
        ).chain(intents -> {
            log.info("Found {} intents without embeddings", intents.size());
            return Multi.createFrom().iterable(intents)
                    .onItem().transformToUniAndConcatenate(intent ->
                            generateAndSaveEmbedding(intent.getId(), intent.getIntentName(), intent.getExamplePhrases())
                    )
                    .collect().asList()
                    .map(results -> {
                        long successful = results.stream().filter(Boolean::booleanValue).count();
                        long failed = results.size() - successful;
                        if (failed > 0) {
                            log.warn("Generated {} embeddings successfully, {} failed", successful, failed);
                        } else {
                            log.info("Generated {} embeddings successfully", successful);
                        }
                        return (int) successful;
                    });
        });
    }

    @Override
    public Uni<Integer> regenerateAllEmbeddings() {
        log.info("Starting regeneration of all embeddings");

        return Panache.withTransaction(() ->
                intentPersistence.findAllActiveIntents()
                        .collect().asList()
        ).chain(intents -> {
            log.info("Found {} active intents to regenerate", intents.size());
            return Multi.createFrom().iterable(intents)
                    .onItem().transformToUniAndConcatenate(intent ->
                            generateAndSaveEmbedding(intent.getId(), intent.getIntentName(), intent.getExamplePhrases())
                    )
                    .collect().asList()
                    .map(results -> {
                        long successful = results.stream().filter(Boolean::booleanValue).count();
                        long failed = results.size() - successful;
                        if (failed > 0) {
                            log.warn("Regenerated {} embeddings successfully, {} failed", successful, failed);
                        } else {
                            log.info("Regenerated {} embeddings successfully", successful);
                        }
                        return (int) successful;
                    });
        });
    }

    @Override
    public Uni<Boolean> generateEmbeddingForIntent(String intentName) {
        log.info("Generating embedding for intent: {}", intentName);

        return Panache.withTransaction(() ->
                intentPersistence.findByIntentName(intentName)
                        .onItem().ifNull().failWith(() ->
                                new IllegalArgumentException("Intent not found: " + intentName))
        ).chain(intent ->
                generateAndSaveEmbedding(intent.getId(), intent.getIntentName(), intent.getExamplePhrases())
        );
    }

    /**
     * Genera embedding para un intent promediando los embeddings de sus frases de ejemplo.
     * FULLY REACTIVE - Los logs se envían de forma asincrónica sin bloquear
     *
     * @param intentId ID del intent (para recarga en transacción de escritura)
     * @param intentName Nombre del intent (para logging)
     * @param examplePhrases Frases de ejemplo para generar embedding
     */
    private Uni<Boolean> generateAndSaveEmbedding(Long intentId, String intentName, String[] examplePhrases) {
        log.info("🔄 [{}] Starting embedding generation - {} example phrases", intentName,
                examplePhrases != null ? examplePhrases.length : 0);

        if (examplePhrases == null || examplePhrases.length == 0) {
            log.warn("⚠️  [{}] No example phrases, skipping", intentName);
            return debugLogger.logComplete(intentName, false)
                    .replaceWith(false);
        }

        return debugLogger.logStart(intentName, examplePhrases.length)
                .chain(v -> generateEmbeddingsForPhrases(intentName, examplePhrases))
                .chain(embeddings -> averageAndSaveEmbedding(intentName, intentId, embeddings))
                .onFailure().recoverWithUni(error -> {
                    log.error("❌ [{}] FAILED to generate embedding", intentName, error);
                    return debugLogger.logError(intentName, "UNKNOWN", error)
                            .replaceWith(false);
                })
                .invoke(success -> {
                    if (success) {
                        debugLogger.logComplete(intentName, true)
                                .subscribe().with(v -> {});
                    }
                });
    }

    private Uni<List<float[]>> generateEmbeddingsForPhrases(String intentName, String[] examplePhrases) {
        return Multi.createFrom().iterable(Arrays.asList(examplePhrases))
                .onItem().transformToUniAndConcatenate(phrase ->
                        embeddingAdapter.generateEmbedding(phrase)
                                .invoke(emb -> {
                                    debugLogger.logEmbeddingGenerated(intentName, phrase, emb.length)
                                            .subscribe().with(v -> {});
                                    log.debug("[{}] ✓ Generated embedding: {} dims", intentName, emb.length);
                                })
                )
                .collect().asList()
                .invoke(embeddings -> {
                    log.info("[{}] Generated {} embeddings, averaging...", intentName, embeddings.size());
                    debugLogger.logAveraged(intentName, embeddings.size(),
                            embeddings.isEmpty() ? 0 : embeddings.get(0).length)
                            .subscribe().with(v -> {});
                });
    }

    private Uni<Boolean> averageAndSaveEmbedding(String intentName, Long intentId, List<float[]> embeddings) {
        float[] averageEmbedding = averageEmbeddings(embeddings);

        String fromThread = Thread.currentThread().getName();

        return debugLogger.logTransactionStart(intentName)
                .chain(v -> Uni.createFrom().item(() -> {
                    log.info("[{}] 💾 Starting DB transaction to save embedding", intentName);
                    return averageEmbedding;
                }))
                .emitOn(command -> {
                    vertx.getOrCreateContext().runOnContext(v -> {
                        String toThread = Thread.currentThread().getName();
                        debugLogger.logThreadSwitch(intentName, fromThread, toThread)
                                .subscribe().with(vv -> {});
                        command.run();
                    });
                })
                .chain(avgEmb -> Panache.withTransaction(() ->
                    intentPersistence.updateEmbedding(intentId, avgEmb)
                            .chain(rowsUpdated -> {
                                if (rowsUpdated == 0) {
                                    return Uni.createFrom().failure(
                                            new IllegalStateException("Intent disappeared: " + intentName));
                                }
                                log.info("[{}] ✅ Updated embedding ({} dimensions) - {} rows affected",
                                        intentName, avgEmb.length, rowsUpdated);
                                debugLogger.logEmbeddingSet(intentName, avgEmb.length)
                                        .subscribe().with(v -> {});
                                debugLogger.logTransactionCommit(intentName)
                                        .subscribe().with(v -> {});
                                return Uni.createFrom().item(true);
                            })
                ))
                .onFailure().recoverWithItem(false);
    }

    /**
     * Promedia múltiples embeddings para crear un embedding representativo
     */
    private float[] averageEmbeddings(List<float[]> embeddings) {
        if (embeddings.isEmpty()) {
            throw new IllegalArgumentException("No embeddings to average");
        }

        int dimensions = embeddings.getFirst().length;
        float[] averaged = new float[dimensions];

        // Sumar todos los embeddings
        for (float[] embedding : embeddings) {
            for (int i = 0; i < dimensions; i++) {
                averaged[i] += embedding[i];
            }
        }

        // Dividir por el número de embeddings para obtener el promedio
        for (int i = 0; i < dimensions; i++) {
            averaged[i] /= embeddings.size();
        }

        return averaged;
    }
}
