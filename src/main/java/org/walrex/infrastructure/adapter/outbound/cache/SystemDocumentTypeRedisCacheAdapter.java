package org.walrex.infrastructure.adapter.outbound.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.walrex.application.dto.response.PagedResponse;
import org.walrex.application.dto.response.SystemDocumentTypeResponse;
import org.walrex.application.port.output.SystemDocumentTypeCachePort;
import org.walrex.infrastructure.adapter.outbound.cache.qualifier.SystemDocumentTypeCache;

/**
 * Implementación de cache para SystemDocumentType usando Redis.
 *
 * Extiende de RedisCacheAdapter para reutilizar la lógica común de cache.
 */
@Slf4j
@ApplicationScoped
@SystemDocumentTypeCache
public class SystemDocumentTypeRedisCacheAdapter
        extends RedisCacheAdapter<SystemDocumentTypeResponse>
        implements SystemDocumentTypeCachePort {

    // Constructor sin argumentos requerido por CDI
    public SystemDocumentTypeRedisCacheAdapter() {
        super();
    }

    @Inject
    public SystemDocumentTypeRedisCacheAdapter(
            ReactiveRedisDataSource redisDataSource,
            ObjectMapper objectMapper) {
        super(
            redisDataSource,
            objectMapper,
            new TypeReference<PagedResponse<SystemDocumentTypeResponse>>() {},
            "SystemDocumentType"
        );
    }

    @Override
    protected String getInvalidationPattern() {
        return SystemDocumentTypeCacheKeyGenerator.getInvalidationPattern();
    }
}