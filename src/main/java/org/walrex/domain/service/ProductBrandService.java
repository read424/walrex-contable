package org.walrex.domain.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.walrex.application.dto.response.ProductBrandResponse;
import org.walrex.application.port.input.CreateProductBrandUseCase;
import org.walrex.application.port.input.ListAllProductBrandUseCase;
import org.walrex.application.port.input.ListProductBrandUseCase;
import org.walrex.application.port.output.ProductBrandCachePort;
import org.walrex.application.port.output.ProductBrandRepositoryPort;
import org.walrex.domain.model.ProductBrand;
import org.walrex.infrastructure.adapter.inbound.mapper.ProductBrandMapper;
import org.walrex.infrastructure.adapter.outbound.cache.ProductBrandCacheKeyGenerator;
import org.walrex.infrastructure.adapter.outbound.cache.qualifier.ProductBrandCache;

import java.time.Duration;
import java.util.List;

@Slf4j
@ApplicationScoped
public class ProductBrandService implements CreateProductBrandUseCase, ListProductBrandUseCase, ListAllProductBrandUseCase {

    @Inject
    ProductBrandRepositoryPort productBrandRepositoryPort;

    @Inject
    @ProductBrandCache
    ProductBrandCachePort brandCachePort;

    @Inject
    ProductBrandMapper brandMapper;

    // TTL del cache: 15 minutos para listado completo (/all endpoint)
    private static final Duration CACHE_ALL_TTL = Duration.ofMinutes(15);

    @Override
    public Uni<ProductBrand> createProductBrand(ProductBrand productBrand) {
        return productBrandRepositoryPort.save(productBrand)
                // Invalidar cache después de crear
                .call(saved -> {
                    log.debug("Invalidating product brand cache after creation");
                    return brandCachePort.invalidateAll();
                });
    }

    @Override
    public Uni<List<ProductBrand>> listProductBrands() {
        return productBrandRepositoryPort.findAll();
    }

    // ==================== ListAllProductBrandUseCase ====================

    /**
     * Obtiene todas las marcas de producto activas sin paginación.
     *
     * Implementa cache-aside pattern con TTL de 15 minutos:
     * 1. Intenta obtener del cache
     * 2. Si no existe, consulta la DB
     * 3. Cachea el resultado por 15 minutos
     * 4. Devuelve el resultado
     *
     * El cache se invalida automáticamente en create.
     *
     * @return Uni con lista completa de marcas
     */
    @Override
    public Uni<List<ProductBrandResponse>> findAll() {
        log.info("Listing all product brands");

        // Generar clave única de cache
        String cacheKey = ProductBrandCacheKeyGenerator.generateKeyForActive();

        // Cache-aside pattern
        return brandCachePort.<ProductBrandResponse>getList(cacheKey)
                .onItem().transformToUni(cachedResult -> {
                    if (cachedResult != null) {
                        log.debug("Returning cached result for key: {}", cacheKey);
                        return Uni.createFrom().item(cachedResult);
                    }

                    // Cache miss - consultar DB
                    log.debug("Cache miss for key: {}. Querying database.", cacheKey);
                    return fetchAllAndCache(cacheKey);
                });
    }

    /**
     * Consulta la DB y cachea el resultado para endpoint /all.
     */
    private Uni<List<ProductBrandResponse>> fetchAllAndCache(String cacheKey) {
        return productBrandRepositoryPort.findAll()
                .onItem().transform(brands ->
                        brands.stream()
                                .map(brandMapper::toResponse)
                                .toList()
                )
                .call(result -> {
                    // Cachear el resultado por 15 minutos (fire-and-forget)
                    log.debug("Caching result for key: {} with TTL of {} minutes",
                            cacheKey, CACHE_ALL_TTL.toMinutes());
                    return brandCachePort.putList(cacheKey, result, CACHE_ALL_TTL);
                });
    }
}
