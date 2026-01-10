package org.walrex.infrastructure.adapter.outbound.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.value.ReactiveValueCommands;
import io.quarkus.redis.datasource.value.SetArgs;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.walrex.application.port.output.AzureAnalysisCachePort;
import org.walrex.domain.model.DocumentAnalysisResult;

import java.time.Duration;

@Slf4j
@ApplicationScoped
public class RedisAzureAnalysisCacheAdapter implements AzureAnalysisCachePort {

    private static final String KEY_PREFIX = "rag:azure:";
    private static final Duration DEFAULT_TTL = Duration.ofDays(30);

    private final ReactiveValueCommands<String, String> valueCommands;
    private final ObjectMapper objectMapper;

    @Inject
    public RedisAzureAnalysisCacheAdapter(
            ReactiveRedisDataSource redisDataSource,
            ObjectMapper objectMapper
    ) {
        this.valueCommands = redisDataSource.value(String.class, String.class);
        this.objectMapper = objectMapper;
    }

    @Override
    public Uni<DocumentAnalysisResult> get(String imageHash) {
        String key = KEY_PREFIX + imageHash;

        return valueCommands.get(key)
                .onItem().transform(json -> {
                    if (json == null) {
                        log.debug("Azure AI cache miss for key: {}", key);
                        return null;
                    }

                    try {
                        DocumentAnalysisResult result =
                                objectMapper.readValue(json, DocumentAnalysisResult.class);
                        log.debug("Azure AI cache hit for key: {}", key);
                        return result;
                    } catch (Exception e) {
                        log.error("Error deserializing Azure AI result for key: {}", key, e);
                        return null;
                    }
                });
    }

    @Override
    public Uni<Void> put(String imageHash, DocumentAnalysisResult result) {
        String key = KEY_PREFIX + imageHash;

        return Uni.createFrom().item(() -> {
                    try {
                        return objectMapper.writeValueAsString(result);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to serialize Azure AI result", e);
                    }
                })
                .chain(json ->
                        valueCommands.set(key, json, new SetArgs().ex(DEFAULT_TTL))
                )
                .invoke(() ->
                        log.debug("Cached Azure AI result for key: {} with TTL: {}", key, DEFAULT_TTL)
                )
                .replaceWithVoid();
    }

    @Override
    public Uni<Void> invalidate(String imageHash) {
        String key = KEY_PREFIX + imageHash;

        return valueCommands
                .getdel(key)
                .invoke(() ->
                        log.debug("Invalidated Azure AI cache for key: {}", key)
                )
                .replaceWithVoid();
    }
}
