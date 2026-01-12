package org.walrex.infrastructure.adapter.outbound.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.walrex.application.dto.response.PagedResponse;
import org.walrex.application.dto.response.ProductAttributeValueResponse;
import org.walrex.application.port.output.ProductAttributeValueCachePort;
import org.walrex.infrastructure.adapter.outbound.cache.qualifier.ProductAttributeValueCache;

/**
 * Implementación concreta de cache Redis para ProductAttributeValue.
 *
 * Extiende la clase genérica RedisCacheAdapter con ProductAttributeValueResponse
 * e implementa ProductAttributeValueCachePort para inyección de dependencias.
 */
@ApplicationScoped
@ProductAttributeValueCache
public class ProductAttributeValueRedisCacheAdapter extends RedisCacheAdapter<ProductAttributeValueResponse> implements ProductAttributeValueCachePort {

    // Constructor sin argumentos requerido por CDI para proxies
    protected ProductAttributeValueRedisCacheAdapter() {
        super();
    }

    @Inject
    public ProductAttributeValueRedisCacheAdapter(ReactiveRedisDataSource reactiveRedisDataSource, ObjectMapper objectMapper) {
        super(
            reactiveRedisDataSource,
            objectMapper,
            new TypeReference<PagedResponse<ProductAttributeValueResponse>>() {},
            "ProductAttributeValue"
        );
    }

    @Override
    protected String getInvalidationPattern() {
        return ProductAttributeValueCacheKeyGenerator.getInvalidationPattern();
    }
}
