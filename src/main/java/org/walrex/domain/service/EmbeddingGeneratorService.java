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
    EmbeddingDebugLogger debugLogger;

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
     * Patrón: Genera embeddings (blocking) → Recarga entity en nueva TX → Persiste
     *
     * @param intentId ID del intent (para recarga en transacción de escritura)
     * @param intentName Nombre del intent (para logging)
     * @param examplePhrases Frases de ejemplo para generar embedding
     */
    private Uni<Boolean> generateAndSaveEmbedding(Long intentId, String intentName, String[] examplePhrases) {
        debugLogger.logStart(intentName, examplePhrases != null ? examplePhrases.length : 0);
        log.info("🔄 [{}] Starting embedding generation - {} example phrases", intentName,
                examplePhrases != null ? examplePhrases.length : 0);

        if (examplePhrases == null || examplePhrases.length == 0) {
            log.warn("⚠️  [{}] No example phrases, skipping", intentName);
            debugLogger.logComplete(intentName, false);
            return Uni.createFrom().item(false);
        }

        // 1. Generar embeddings (FUERA de transacción - incluye llamadas bloqueantes a Ollama)
        return Multi.createFrom().iterable(Arrays.asList(examplePhrases))
                .onItem().transformToUniAndConcatenate(phrase ->
                        embeddingAdapter.generateEmbedding(phrase)  // Ejecuta en worker pool
                                .invoke(emb -> {
                                    debugLogger.logEmbeddingGenerated(intentName, phrase, emb.length);
                                    log.debug("[{}] ✓ Generated embedding: {} dims", intentName, emb.length);
                                })
                )
                .collect().asList()
                .invoke(embeddings -> {
                    debugLogger.logAveraged(intentName, embeddings.size(),
                            embeddings.isEmpty() ? 0 : embeddings.get(0).length);
                    log.info("[{}] Generated {} embeddings, averaging...", intentName, embeddings.size());
                })
                .map(this::averageEmbeddings)
                // 2. Volver explícitamente al event-loop de Vert.x antes de DB operation
                .emitOn(command -> {
                    String fromThread = Thread.currentThread().getName();
                    vertx.getOrCreateContext().runOnContext(v -> {
                        String toThread = Thread.currentThread().getName();
                        debugLogger.logThreadSwitch(intentName, fromThread, toThread);
                        command.run();
                    });
                })
                // 3. Persistir en nueva transacción dedicada (recargar entity + actualizar)
                .chain(averageEmbedding -> {
                    debugLogger.logTransactionStart(intentName);
                    log.info("[{}] 💾 Starting DB transaction to save embedding", intentName);

                    // Usar UPDATE nativo para evitar bug de Hibernate Reactive con PGvector
                    return Panache.withTransaction(() -> {
                        return intentPersistence.updateEmbedding(intentId, averageEmbedding)
                                .invoke(rowsUpdated -> {
                                    debugLogger.logEmbeddingSet(intentName, averageEmbedding.length);
                                    log.info("[{}] ✅ Updated embedding ({} dimensions) - {} rows affected",
                                            intentName, averageEmbedding.length, rowsUpdated);
                                })
                                .chain(rowsUpdated -> {
                                    if (rowsUpdated == 0) {
                                        return Uni.createFrom().failure(
                                                new IllegalStateException("Intent disappeared: " + intentName));
                                    }
                                    debugLogger.logTransactionCommit(intentName);
                                    log.info("[{}] ✅ Transaction committed successfully", intentName);
                                    return Uni.createFrom().item(rowsUpdated);
                                });
                    })
                    .map(rowsUpdated -> true);
                })
                .onFailure().invoke(error -> {
                    debugLogger.logError(intentName, "UNKNOWN", error);
                    log.error("❌ [{}] FAILED to generate embedding", intentName, error);
                })
                .onFailure().recoverWithItem(error -> {
                    debugLogger.logComplete(intentName, false);
                    log.error("❌ [{}] Recovering from failure, returning false", intentName);
                    return false;
                })
                .invoke(success -> {
                    if (success) {
                        debugLogger.logComplete(intentName, true);
                    }
                });
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
