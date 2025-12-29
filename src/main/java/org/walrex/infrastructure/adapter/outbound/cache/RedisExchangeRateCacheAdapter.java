package org.walrex.infrastructure.adapter.outbound.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.value.ReactiveValueCommands;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.walrex.application.port.output.ExchangeRateCachePort;
import org.walrex.domain.model.ExchangeRateCache;

import java.time.Duration;
import java.util.Optional;

/**
 * Adaptador que implementa ExchangeRateCachePort usando Redis.
 *
 * Utiliza el cliente reactivo de Quarkus Redis para operaciones asíncronas
 * y serialización JSON con Jackson.
 */
@Slf4j
@ApplicationScoped
@RegisterForReflection
public class RedisExchangeRateCacheAdapter implements ExchangeRateCachePort {

    private final ReactiveValueCommands<String, String> valueCommands;
    private final ObjectMapper objectMapper;

    public RedisExchangeRateCacheAdapter(ReactiveRedisDataSource dataSource) {
        this.valueCommands = dataSource.value(String.class, String.class);
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public Uni<Optional<ExchangeRateCache>> get(String key) {
        log.debug("Getting exchange rate from cache: key={}", key);

        return valueCommands.get(key)
                .map(json -> {
                    if (json == null) {
                        log.debug("Cache miss for key={}", key);
                        return Optional.<ExchangeRateCache>empty();
                    }

                    try {
                        ExchangeRateCache cache = objectMapper.readValue(json, ExchangeRateCache.class);
                        log.debug("Cache hit for key={}, rate={}", key, cache.getRate());
                        return Optional.of(cache);
                    } catch (JsonProcessingException e) {
                        log.error("Error deserializing cache value for key={}: {}", key, e.getMessage());
                        return Optional.<ExchangeRateCache>empty();
                    }
                })
                .onFailure().invoke(error ->
                        log.error("Redis error getting key={}: {}", key, error.getMessage())
                )
                .onFailure().recoverWithItem(Optional.empty());
    }

    @Override
    public Uni<Void> set(String key, ExchangeRateCache value, Duration ttl) {
        log.debug("Setting exchange rate in cache: key={}, rate={}, ttl={}",
                key, value.getRate(), ttl);

        try {
            String json = objectMapper.writeValueAsString(value);

            return valueCommands.setex(key, ttl.getSeconds(), json)
                    .replaceWithVoid()
                    .invoke(() -> log.debug("Cache set successfully: key={}", key))
                    .onFailure().invoke(error ->
                            log.error("Redis error setting key={}: {}", key, error.getMessage())
                    )
                    .onFailure().recoverWithNull();
        } catch (JsonProcessingException e) {
            log.error("Error serializing cache value for key={}: {}", key, e.getMessage());
            return Uni.createFrom().voidItem();
        }
    }

    @Override
    public Uni<Boolean> delete(String key) {
        log.debug("Deleting exchange rate from cache: key={}", key);

        return valueCommands.getdel(key)
                .map(deleted -> deleted != null)
                .invoke(deleted -> log.debug("Cache deletion for key={}: {}", key,
                        deleted ? "success" : "not found"))
                .onFailure().invoke(error ->
                        log.error("Redis error deleting key={}: {}", key, error.getMessage())
                )
                .onFailure().recoverWithItem(false);
    }
}
