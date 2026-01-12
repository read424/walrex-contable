package org.walrex.infrastructure.adapter.outbound.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.walrex.application.dto.response.PagedResponse;
import org.walrex.application.dto.response.ProductCategoryUomResponse;
import org.walrex.application.port.output.ProductCategoryUomCachePort;
import org.walrex.infrastructure.adapter.outbound.cache.qualifier.ProductCategoryUomCache;

/**
 * Implementación concreta de cache Redis para ProductCategoryUom.
 *
 * Extiende la clase genérica RedisCacheAdapter con ProductCategoryUomResponse
 * e implementa ProductCategoryUomCachePort para inyección de dependencias.
 */
@ApplicationScoped
@ProductCategoryUomCache
public class ProductCategoryUomRedisCacheAdapter extends RedisCacheAdapter<ProductCategoryUomResponse> implements ProductCategoryUomCachePort {

    // Constructor sin argumentos requerido por CDI para proxies
    protected ProductCategoryUomRedisCacheAdapter() {
        super();
    }

    @Inject
    public ProductCategoryUomRedisCacheAdapter(ReactiveRedisDataSource reactiveRedisDataSource, ObjectMapper objectMapper) {
        super(
            reactiveRedisDataSource,
            objectMapper,
            new TypeReference<PagedResponse<ProductCategoryUomResponse>>() {},
            "ProductCategoryUom"
        );
    }

    @Override
    protected String getInvalidationPattern() {
        return ProductCategoryUomCacheKeyGenerator.getInvalidationPattern();
    }
}
