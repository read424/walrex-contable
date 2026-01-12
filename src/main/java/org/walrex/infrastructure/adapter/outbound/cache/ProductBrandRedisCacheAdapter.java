package org.walrex.infrastructure.adapter.outbound.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.walrex.application.dto.response.PagedResponse;
import org.walrex.application.dto.response.ProductBrandResponse;
import org.walrex.application.port.output.ProductBrandCachePort;
import org.walrex.infrastructure.adapter.outbound.cache.qualifier.ProductBrandCache;

/**
 * Implementación de caché Redis para ProductBrand.
 *
 * Siguiendo el patrón hexagonal, este adaptador:
 * - Implementa ProductBrandCachePort (puerto de salida)
 * - Extiende RedisCacheAdapter para reutilizar lógica común
 * - Se marca con @ProductBrandCache para inyección específica
 *
 * Configuración:
 * - TypeReference: Define el tipo específico para serialización/deserialización
 * - Patrón de invalidación: "product-brand:*" (invalida todas las claves de marcas)
 */
@ApplicationScoped
@ProductBrandCache
public class ProductBrandRedisCacheAdapter extends RedisCacheAdapter<ProductBrandResponse> implements ProductBrandCachePort {

    // Constructor sin argumentos requerido por CDI
    public ProductBrandRedisCacheAdapter() {
        super();
    }

    @Inject
    public ProductBrandRedisCacheAdapter(
            ReactiveRedisDataSource reactiveRedisDataSource,
            ObjectMapper objectMapper) {
        super(
                reactiveRedisDataSource,
                objectMapper,
                new TypeReference<PagedResponse<ProductBrandResponse>>() {},
                "ProductBrand"
        );
    }

    @Override
    protected String getInvalidationPattern() {
        return ProductBrandCacheKeyGenerator.getInvalidationPattern();
    }
}
