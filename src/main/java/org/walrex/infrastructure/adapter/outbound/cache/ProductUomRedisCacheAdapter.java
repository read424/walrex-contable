package org.walrex.infrastructure.adapter.outbound.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.walrex.application.dto.response.PagedResponse;
import org.walrex.application.dto.response.ProductUomResponse;
import org.walrex.application.port.output.ProductUomCachePort;
import org.walrex.infrastructure.adapter.outbound.cache.qualifier.ProductUomCache;

/**
 * Implementación de caché Redis para ProductUom.
 *
 * Siguiendo el patrón hexagonal, este adaptador:
 * - Implementa ProductUomCachePort (puerto de salida)
 * - Extiende RedisCacheAdapter para reutilizar lógica común
 * - Se marca con @ProductUomCache para inyección específica
 *
 * Configuración:
 * - TypeReference: Define el tipo específico para serialización/deserialización
 * - Patrón de invalidación: "product-uom:*" (invalida list y all)
 */
@ApplicationScoped
@ProductUomCache
public class ProductUomRedisCacheAdapter extends RedisCacheAdapter<ProductUomResponse> implements ProductUomCachePort {

    // Constructor sin argumentos requerido por CDI
    public ProductUomRedisCacheAdapter() {
        super();
    }

    @Inject
    public ProductUomRedisCacheAdapter(
            ReactiveRedisDataSource reactiveRedisDataSource,
            ObjectMapper objectMapper) {
        super(
                reactiveRedisDataSource,
                objectMapper,
                new TypeReference<PagedResponse<ProductUomResponse>>() {},
                "ProductUom"
        );
    }

    @Override
    protected String getInvalidationPattern() {
        return ProductUomCacheKeyGenerator.getInvalidationPattern();
    }
}
