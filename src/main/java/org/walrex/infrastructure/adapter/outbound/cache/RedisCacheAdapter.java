package org.walrex.infrastructure.adapter.outbound.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.value.ReactiveValueCommands;
import io.smallrye.mutiny.Uni;
import lombok.extern.slf4j.Slf4j;
import org.walrex.application.dto.response.PagedResponse;
import org.walrex.application.port.output.CachePort;

import java.time.Duration;

/**
 * Implementación genérica de cache usando Redis.
 *
 * Siguiendo el patrón hexagonal, este adapter implementa el puerto
 * de cache usando Redis como tecnología de almacenamiento.
 *
 * Features:
 * - Almacenamiento reactivo con Mutiny
 * - Serialización/deserialización JSON
 * - TTL configurable
 * - Manejo de errores graceful (si Redis falla, no afecta la app)
 * - Genérico para cualquier tipo de respuesta
 *
 * @param <T> Tipo de respuesta que se almacenará en caché
 */
@Slf4j
public abstract class RedisCacheAdapter<T> implements CachePort<T> {

    protected ReactiveValueCommands<String, String> valueCommands;
    protected ReactiveRedisDataSource redisDataSource;
    protected ObjectMapper objectMapper;
    protected TypeReference<PagedResponse<T>> typeReference;
    protected String entityName;

    // Constructor sin argumentos requerido por CDI para proxies
    protected RedisCacheAdapter() {
    }

    protected RedisCacheAdapter(
            ReactiveRedisDataSource reactiveRedisDataSource,
            ObjectMapper objectMapper,
            TypeReference<PagedResponse<T>> typeReference,
            String entityName) {
        this.redisDataSource = reactiveRedisDataSource;
        this.valueCommands = reactiveRedisDataSource.value(String.class, String.class);
        this.objectMapper = objectMapper;
        this.typeReference = typeReference;
        this.entityName = entityName;
    }

    @Override
    public Uni<PagedResponse<T>> get(String cacheKey) {
        log.debug("[{}] Attempting to get from cache with key: {}", entityName, cacheKey);

        return valueCommands.get(cacheKey)
                .onItem().transformToUni(jsonValue -> {
                    if (jsonValue == null) {
                        log.debug("[{}] Cache miss for key: {}", entityName, cacheKey);
                        return Uni.createFrom().nullItem();
                    }

                    try {
                        PagedResponse<T> result = deserialize(jsonValue);
                        log.debug("[{}] Cache hit for key: {}", entityName, cacheKey);
                        return Uni.createFrom().item(result);
                    } catch (Exception e) {
                        log.warn("[{}] Error deserializing cached value for key: {}. Error: {}", entityName, cacheKey, e.getMessage());
                        // Si falla la deserialización, invalidar la clave corrupta
                        return invalidate(cacheKey)
                                .replaceWith(Uni.createFrom().nullItem());
                    }
                })
                .onFailure().invoke(error ->
                        log.warn("[{}] Error accessing cache for key: {}. Error: {}", entityName, cacheKey, error.getMessage())
                )
                .onFailure().recoverWithNull(); // Si Redis falla, continuar sin cache
    }

    @Override
    public Uni<Void> put(String cacheKey, PagedResponse<T> value, Duration ttl) {
        log.debug("[{}] Caching result with key: {} and TTL: {}", entityName, cacheKey, ttl);

        try {
            String jsonValue = serialize(value);

            return valueCommands.setex(cacheKey, ttl.getSeconds(), jsonValue)
                    .onItem().invoke(() ->
                            log.debug("[{}] Successfully cached result for key: {}", entityName, cacheKey)
                    )
                    .onFailure().invoke(error ->
                            log.warn("[{}] Error caching value for key: {}. Error: {}", entityName, cacheKey, error.getMessage())
                    )
                    .onFailure().recoverWithNull() // Si falla, continuar sin cache
                    .replaceWithVoid();

        } catch (Exception e) {
            log.warn("[{}] Error serializing value for cache key: {}. Error: {}", entityName, cacheKey, e.getMessage());
            return Uni.createFrom().voidItem();
        }
    }

    @Override
    public Uni<Void> invalidate(String cacheKey) {
        log.debug("[{}] Invalidating cache key: {}", entityName, cacheKey);

        return valueCommands.getdel(cacheKey)
                .onItem().invoke(() ->
                        log.debug("[{}] Successfully invalidated key: {}", entityName, cacheKey)
                )
                .onFailure().invoke(error ->
                        log.warn("[{}] Error invalidating cache key: {}. Error: {}", entityName, cacheKey, error.getMessage())
                )
                .onFailure().recoverWithNull()
                .replaceWithVoid();
    }

    /**
     * Método abstracto para obtener el patrón de invalidación.
     * Cada implementación concreta debe proporcionar su propio patrón.
     *
     * @return Patrón de claves para invalidar (ej: "currency:*")
     */
    protected abstract String getInvalidationPattern();

    @Override
    public Uni<Void> invalidateAll() {
        String pattern = getInvalidationPattern();
        log.info("[{}] Invalidating all cache entries with pattern: {}", entityName, pattern);

        // Nota: En producción, considera usar SCAN en lugar de KEYS para mejor rendimiento
        return redisDataSource
                .key()
                .keys(pattern)
                .onItem().transformToUni(keys -> {
                    if (keys.isEmpty()) {
                        log.debug("[{}] No cache keys found to invalidate", entityName);
                        return Uni.createFrom().voidItem();
                    }

                    log.debug("[{}] Found {} cache keys to invalidate", entityName, keys.size());

                    // Eliminar todas las claves encontradas
                    return redisDataSource
                            .key()
                            .del(keys.toArray(new String[0]))
                            .onItem().invoke(deleted ->
                                    log.info("[{}] Invalidated {} cache entries", entityName, deleted)
                            )
                            .replaceWithVoid();
                })
                .onFailure().invoke(error ->
                        log.warn("[{}] Error invalidating all cache entries. Error: {}", entityName, error.getMessage())
                )
                .onFailure().recoverWithNull()
                .replaceWithVoid();
    }

    /**
     * Serializa el objeto PagedResponse a JSON.
     */
    protected String serialize(PagedResponse<T> value) throws JsonProcessingException {
        return objectMapper.writeValueAsString(value);
    }

    /**
     * Deserializa JSON a PagedResponse.
     */
    protected PagedResponse<T> deserialize(String json) throws JsonProcessingException {
        return objectMapper.readValue(json, typeReference);
    }
}