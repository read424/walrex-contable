package org.walrex.infrastructure.adapter.outbound.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.value.ReactiveValueCommands;
import io.quarkus.redis.datasource.value.SetArgs;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.walrex.application.port.output.EmbeddingCachePort;
import org.walrex.domain.model.AccountingBookType;
import org.walrex.domain.model.CachedEmbedding;

import java.time.Duration;

@Slf4j
@ApplicationScoped
public class RedisEmbeddingCacheAdapter  implements EmbeddingCachePort {

    private static final String KEY_PREFIX = "rag:embedding:";
    private static final Duration DEFAULT_TTL = Duration.ofDays(30);

    private final ReactiveValueCommands<String, String> valueCommands;
    private final ObjectMapper objectMapper;

    @Inject
    public RedisEmbeddingCacheAdapter(
            ReactiveRedisDataSource redisDataSource,
            ObjectMapper objectMapper
    ) {
        this.valueCommands = redisDataSource.value(String.class, String.class);
        this.objectMapper = objectMapper;
    }



    @Override
    public Uni<CachedEmbedding> get(String imageHash, AccountingBookType bookType) {
        String key = buildKey(imageHash, bookType);

        return valueCommands.get(key)
                .onItem().transform(json -> {
                    if (json == null) {
                        log.debug("Embedding cache miss for key: {}", key);
                        return null;
                    }

                    try {
                        CachedEmbedding result =
                                objectMapper.readValue(json, CachedEmbedding.class);
                        log.debug("Embedding cache hit for key: {}", key);
                        return result;
                    } catch (Exception e) {
                        log.error("Error deserializing embedding for key: {}", key, e);
                        return null;
                    }
                });
    }

    @Override
    public Uni<Void> put(String imageHash,
                         AccountingBookType bookType,
                         CachedEmbedding cachedEmbedding) {

        String key = buildKey(imageHash, bookType);

        return Uni.createFrom().item(() -> {
                    try {
                        return objectMapper.writeValueAsString(cachedEmbedding);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to serialize embedding", e);
                    }
                })
                .chain(json ->
                        valueCommands.set(key, json, new SetArgs().ex(DEFAULT_TTL))
                )
                .invoke(() ->
                        log.debug("Cached embedding for key: {} with TTL: {}", key, DEFAULT_TTL)
                )
                .replaceWithVoid();
    }

    @Override
    public Uni<Void> invalidate(String imageHash, AccountingBookType bookType) {
        String key = buildKey(imageHash, bookType);

        return valueCommands
                .getdel(key)
                .invoke(() ->
                        log.debug("Invalidated embedding cache for key: {}", key)
                )
                .replaceWithVoid();
    }

    /**
     * Formato: rag:embedding:{hash}:{book_type}
     */
    private String buildKey(String imageHash, AccountingBookType bookType) {
        return KEY_PREFIX + imageHash + ":" + bookType.name();
    }
}
