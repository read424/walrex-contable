package org.walrex.infrastructure.adapter.outbound.persistence.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.hibernate.reactive.mutiny.Mutiny;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.IntentEmbeddingEntity;

import jakarta.inject.Inject;
import java.util.List;

/**
 * Repositorio para IntentEmbeddingEntity con soporte para búsqueda semántica usando pgvector
 */
@ApplicationScoped
public class IntentEmbeddingRepository implements PanacheRepositoryBase<IntentEmbeddingEntity, Long> {

    @Inject
    Mutiny.SessionFactory sessionFactory;

    /**
     * Busca intent por nombre
     */
    public Uni<IntentEmbeddingEntity> findByIntentName(String intentName) {
        return find("intentName = ?1 and enabled = true", intentName).firstResult();
    }

    /**
     * Busca el intent más similar a un embedding dado usando similitud de coseno.
     *
     * Retorna el intent con mayor similitud (distancia de coseno más baja).
     *
     * @param embedding Vector de 1024 dimensiones
     * @param threshold Umbral mínimo de similitud (0.0 a 1.0), valores más altos = más restrictivo
     * @return Intent más similar o vacío si no supera el threshold
     */
    public Uni<IntentEmbeddingEntity> findMostSimilar(float[] embedding, double threshold) {
        String sql = """
            SELECT * FROM intent_embeddings
            WHERE enabled = true
            AND 1 - (embedding <=> CAST(:embedding AS vector)) > :threshold
            ORDER BY embedding <=> CAST(:embedding AS vector)
            LIMIT 1
            """;

        return sessionFactory.withSession(session ->
            session.createNativeQuery(sql, IntentEmbeddingEntity.class)
                .setParameter("embedding", formatVector(embedding))
                .setParameter("threshold", threshold)
                .getSingleResultOrNull()
        );
    }

    /**
     * Busca los N intents más similares a un embedding dado.
     *
     * @param embedding Vector de 1024 dimensiones
     * @param threshold Umbral mínimo de similitud (0.0 a 1.0)
     * @param limit Número máximo de resultados
     * @return Lista de intents ordenados por similitud (más similar primero)
     */
    public Multi<IntentEmbeddingEntity> findTopSimilar(float[] embedding, double threshold, int limit) {
        String sql = """
            SELECT * FROM intent_embeddings
            WHERE enabled = true
            AND 1 - (embedding <=> CAST(:embedding AS vector)) > :threshold
            ORDER BY embedding <=> CAST(:embedding AS vector)
            LIMIT :limit
            """;

        return sessionFactory.withSession(session ->
            session.createNativeQuery(sql, IntentEmbeddingEntity.class)
                .setParameter("embedding", formatVector(embedding))
                .setParameter("threshold", threshold)
                .setParameter("limit", limit)
                .getResultList()
        ).onItem().transformToMulti(list -> Multi.createFrom().iterable(list));
    }

    /**
     * Busca el intent más similar con score de similitud.
     *
     * @param embedding Vector de 1024 dimensiones
     * @param threshold Umbral mínimo de similitud (0.0 a 1.0)
     * @return DTO con intent y score de similitud
     */
    public Uni<IntentWithScore> findMostSimilarWithScore(float[] embedding, double threshold) {
        String sql = """
            SELECT
                id, intent_name, description,
                tool_name, prompt_template, enabled,
                1 - (embedding <=> CAST(:embedding AS vector)) as similarity_score
            FROM intent_embeddings
            WHERE enabled = true
            AND 1 - (embedding <=> CAST(:embedding AS vector)) > :threshold
            ORDER BY embedding <=> CAST(:embedding AS vector)
            LIMIT 1
            """;

        return sessionFactory.withSession(session ->
            session.createNativeQuery(sql)
                .setParameter("embedding", formatVector(embedding))
                .setParameter("threshold", threshold)
                .getSingleResultOrNull()
                .map(result -> {
                    if (result == null) return null;
                    Object[] row = (Object[]) result;

                    IntentEmbeddingEntity entity = new IntentEmbeddingEntity();
                    entity.setId(((Number) row[0]).longValue());
                    entity.setIntentName((String) row[1]);
                    entity.setDescription((String) row[2]);
                    // No seteamos example_phrases - no lo necesitamos para la respuesta
                    // No seteamos embedding - no lo necesitamos para la respuesta
                    entity.setToolName((String) row[3]);
                    entity.setPromptTemplate((String) row[4]);
                    entity.setEnabled((Boolean) row[5]);
                    // created_at y updated_at no son necesarios para este caso

                    double score = ((Number) row[6]).doubleValue();

                    return new IntentWithScore(entity, score);
                })
        );
    }

    /**
     * Lista todos los intents activos
     */
    public Multi<IntentEmbeddingEntity> findAllActive() {
        return list("enabled = true")
                .onItem()
                .transformToMulti(list -> Multi.createFrom().iterable(list));
    }

    /**
     * Cuenta intents activos
     */
    public Uni<Long> countActive() {
        return count("enabled = true");
    }

    /**
     * Actualiza el embedding de un intent usando query nativa
     * (Workaround para bug de Hibernate Reactive con PGvector en UPDATEs)
     */
    public Uni<Integer> updateEmbedding(Long intentId, float[] embedding) {
        String sql = """
            UPDATE intent_embeddings
            SET embedding = CAST(:embedding AS vector),
                updated_at = CURRENT_TIMESTAMP
            WHERE id = :id
            """;

        return sessionFactory.withSession(session ->
            session.createNativeQuery(sql)
                .setParameter("embedding", formatVector(embedding))
                .setParameter("id", intentId)
                .executeUpdate()
        );
    }

    /**
     * Formatea un array de floats al formato de texto de pgvector
     * Ejemplo: [0.1, 0.2, 0.3]
     */
    private String formatVector(float[] vector) {
        if (vector == null || vector.length == 0) {
            return "[]";
        }

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(vector[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * DTO para retornar intent con su score de similitud
     */
    public record IntentWithScore(IntentEmbeddingEntity intent, double similarityScore) {}
}
