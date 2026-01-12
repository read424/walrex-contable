package org.walrex.infrastructure.adapter.outbound.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.walrex.application.dto.response.PagedResponse;
import org.walrex.application.dto.response.ProductTemplateResponse;
import org.walrex.application.port.output.ProductTemplateCachePort;
import org.walrex.infrastructure.adapter.outbound.cache.qualifier.ProductTemplateCache;

/**
 * Implementación concreta de cache Redis para ProductTemplate.
 *
 * Extiende la clase genérica RedisCacheAdapter con ProductTemplateResponse
 * e implementa ProductTemplateCachePort para inyección de dependencias.
 */
@ApplicationScoped
@ProductTemplateCache
public class ProductTemplateRedisCacheAdapter
        extends RedisCacheAdapter<ProductTemplateResponse>
        implements ProductTemplateCachePort {

    // Constructor sin argumentos requerido por CDI para proxies
    protected ProductTemplateRedisCacheAdapter() {
        super();
    }

    @Inject
    public ProductTemplateRedisCacheAdapter(ReactiveRedisDataSource reactiveRedisDataSource, ObjectMapper objectMapper) {
        super(
            reactiveRedisDataSource,
            objectMapper,
            new TypeReference<PagedResponse<ProductTemplateResponse>>() {},
            "ProductTemplate"
        );
    }

    @Override
    protected String getInvalidationPattern() {
        return ProductTemplateCacheKeyGenerator.getInvalidationPattern();
    }
}
