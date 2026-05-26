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
import org.walrex.application.port.output.MarketPriceCachePort;
import org.walrex.domain.model.MarketPrice;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@ApplicationScoped
@RegisterForReflection
public class MarketPriceRedisCacheAdapter implements MarketPriceCachePort {

    private static final String KEY_PREFIX = "fx:";

    private final ReactiveValueCommands<String, String> valueCommands;
    private final ObjectMapper objectMapper;

    public MarketPriceRedisCacheAdapter(ReactiveRedisDataSource dataSource) {
        this.valueCommands = dataSource.value(String.class, String.class);
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public Uni<Optional<MarketPrice>> get(String symbol) {
        String key = KEY_PREFIX + symbol;
        log.debug("Getting market price from cache: key={}", key);

        return valueCommands.get(key)
                .map(json -> {
                    if (json == null) {
                        log.debug("Cache miss for key={}", key);
                        return Optional.<MarketPrice>empty();
                    }
                    try {
                        MarketPrice price = objectMapper.readValue(json, MarketPrice.class);
                        log.debug("Cache hit for key={}, price={}", key, price.getPrice());
                        return Optional.of(price);
                    } catch (JsonProcessingException e) {
                        log.error("Error deserializing market price for key={}: {}", key, e.getMessage());
                        return Optional.<MarketPrice>empty();
                    }
                })
                .onFailure().invoke(error ->
                        log.error("Redis error getting key={}: {}", key, error.getMessage())
                )
                .onFailure().recoverWithItem(Optional.empty());
    }

    @Override
    public Uni<Void> set(String symbol, MarketPrice price, Duration ttl) {
        String key = KEY_PREFIX + symbol;
        log.debug("Setting market price in cache: key={}, price={}, ttl={}", key, price.getPrice(), ttl);

        try {
            String json = objectMapper.writeValueAsString(price);

            return valueCommands.setex(key, ttl.getSeconds(), json)
                    .replaceWithVoid()
                    .invoke(() -> log.debug("Cache set successfully: key={}", key))
                    .onFailure().invoke(error ->
                            log.error("Redis error setting key={}: {}", key, error.getMessage())
                    )
                    .onFailure().recoverWithNull();
        } catch (JsonProcessingException e) {
            log.error("Error serializing market price for key={}: {}", key, e.getMessage());
            return Uni.createFrom().voidItem();
        }
    }

    @Override
    public Uni<Boolean> delete(String symbol) {
        String key = KEY_PREFIX + symbol;
        log.debug("Deleting market price from cache: key={}", key);

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
