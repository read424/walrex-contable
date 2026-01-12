package org.walrex.infrastructure.adapter.outbound.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.walrex.application.dto.response.PagedResponse;
import org.walrex.application.dto.response.ProductAttributeResponse;
import org.walrex.application.port.output.ProductAttributeCachePort;
import org.walrex.infrastructure.adapter.outbound.cache.qualifier.ProductAttributeCache;

/**
 * Implementación concreta de cache Redis para ProductAttribute.
 *
 * Extiende la clase genérica RedisCacheAdapter con ProductAttributeResponse
 * e implementa ProductAttributeCachePort para inyección de dependencias.
 */
@ApplicationScoped
@ProductAttributeCache
public class ProductAttributeRedisCacheAdapter extends RedisCacheAdapter<ProductAttributeResponse> implements ProductAttributeCachePort {

    // Constructor sin argumentos requerido por CDI para proxies
    protected ProductAttributeRedisCacheAdapter() {
        super();
    }

    @Inject
    public ProductAttributeRedisCacheAdapter(ReactiveRedisDataSource reactiveRedisDataSource, ObjectMapper objectMapper) {
        super(
            reactiveRedisDataSource,
            objectMapper,
            new TypeReference<PagedResponse<ProductAttributeResponse>>() {},
            "ProductAttribute"
        );
    }

    @Override
    protected String getInvalidationPattern() {
        return ProductAttributeCacheKeyGenerator.getInvalidationPattern();
    }
}
