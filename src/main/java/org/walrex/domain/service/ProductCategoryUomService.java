package org.walrex.domain.service;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.walrex.application.dto.query.PageRequest;
import org.walrex.application.dto.query.ProductCategoryUomFilter;
import org.walrex.application.dto.response.PagedResponse;
import org.walrex.application.dto.response.ProductCategoryUomResponse;
import org.walrex.application.dto.response.ProductCategoryUomSelectResponse;
import org.walrex.application.port.input.*;
import org.walrex.application.port.output.ProductCategoryUomCachePort;
import org.walrex.application.port.output.ProductCategoryUomQueryPort;
import org.walrex.application.port.output.ProductCategoryUomRepositoryPort;
import org.walrex.domain.exception.DuplicateProductCategoryUomException;
import org.walrex.domain.exception.ProductCategoryUomNotFoundException;
import org.walrex.domain.model.ProductCategoryUom;
import org.walrex.infrastructure.adapter.inbound.mapper.ProductCategoryUomDtoMapper;
import org.walrex.infrastructure.adapter.outbound.cache.ProductCategoryUomCacheKeyGenerator;
import org.walrex.infrastructure.adapter.outbound.cache.qualifier.ProductCategoryUomCache;

import java.time.Duration;
import java.util.List;

/**
 * Servicio de dominio para categorías de unidades de medida.
 *
 * Implementa todos los casos de uso relacionados con categorías siguiendo
 * el patrón hexagonal. Orquesta la lógica de negocio, validaciones,
 * operaciones de persistencia y caché.
 */
@Slf4j
@Transactional
@ApplicationScoped
public class ProductCategoryUomService implements
        CreateProductCategoryUomUseCase,
        ListProductCategoryUomUseCase,
        ListAllProductCategoryUomUseCase,
        GetProductCategoryUomByIdUseCase,
        UpdateProductCategoryUomUseCase,
        DeleteProductCategoryUomUseCase {

    @Inject
    ProductCategoryUomRepositoryPort categoryRepositoryPort;

    @Inject
    ProductCategoryUomQueryPort categoryQueryPort;

    @Inject
    @ProductCategoryUomCache
    ProductCategoryUomCachePort categoryCachePort;

    @Inject
    ProductCategoryUomDtoMapper categoryDtoMapper;

    // TTL del cache: 5 minutos para listado paginado
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    // TTL del cache: 15 minutos para listado completo (/all endpoint)
    // Mayor TTL porque los datos son más estáticos y se invalida en cada cambio
    private static final Duration CACHE_ALL_TTL = Duration.ofMinutes(15);

    // ==================== CreateProductCategoryUomUseCase ====================

    /**
     * Crea una nueva categoría de unidad de medida.
     *
     * @throws DuplicateProductCategoryUomException si el código o nombre ya existe
     */
    @Override
    public Uni<ProductCategoryUom> execute(ProductCategoryUom productCategoryUom) {
        log.info("Creating product category uom: {} ({})", productCategoryUom.getName(), productCategoryUom.getCode());

        // Validar unicidad de código y nombre
        return validateUniqueness(productCategoryUom.getCode(), productCategoryUom.getName(), null)
                .onItem().transformToUni(v -> categoryRepositoryPort.save(productCategoryUom))
                .call(savedCategory -> {
                    // Invalidar cache después de crear
                    log.debug("Invalidating product category uom cache after creation");
                    return categoryCachePort.invalidateAll();
                });
    }

    // ==================== ListProductCategoryUomUseCase ====================

    /**
     * Lista categorías con paginación y filtros.
     *
     * Implementa cache-aside pattern:
     * 1. Intenta obtener del cache
     * 2. Si no existe, consulta la DB
     * 3. Cachea el resultado
     * 4. Devuelve el resultado
     *
     * @param pageRequest Configuración de paginación (page, size, sort)
     * @param filter Filtros opcionales (search, code, name, etc.)
     * @return Uni con respuesta paginada
     */
    @Override
    public Uni<PagedResponse<ProductCategoryUomResponse>> execute(PageRequest pageRequest, ProductCategoryUomFilter filter) {
        log.info("Listing product category uoms with page: {}, size: {}, filter: {}",
                pageRequest.getPage(), pageRequest.getSize(), filter);

        // Generar clave única de cache
        String cacheKey = ProductCategoryUomCacheKeyGenerator.generateKey(pageRequest, filter);

        // Cache-aside pattern
        return categoryCachePort.get(cacheKey)
                .onItem().transformToUni(cachedResult -> {
                    if (cachedResult != null) {
                        log.debug("Returning cached result for key: {}", cacheKey);
                        return Uni.createFrom().item(cachedResult);
                    }

                    // Cache miss - consultar DB
                    log.debug("Cache miss for key: {}. Querying database.", cacheKey);
                    return fetchFromDatabaseAndCache(pageRequest, filter, cacheKey);
                });
    }

    /**
     * Consulta la DB y cachea el resultado.
     */
    private Uni<PagedResponse<ProductCategoryUomResponse>> fetchFromDatabaseAndCache(
            PageRequest pageRequest,
            ProductCategoryUomFilter filter,
            String cacheKey) {

        return categoryQueryPort.findAll(pageRequest, filter)
                .onItem().transform(pagedResult -> {
                    var responses = pagedResult.content().stream()
                            .map(categoryDtoMapper::toResponse)
                            .toList();

                    // Convert page from 0-based (backend) to 1-based (frontend)
                    return PagedResponse.of(
                            responses,
                            pagedResult.page() + 1,
                            pagedResult.size(),
                            pagedResult.totalElements()
                    );
                })
                .call(result -> {
                    // Cachear el resultado (fire-and-forget)
                    log.debug("Caching result for key: {}", cacheKey);
                    return categoryCachePort.put(cacheKey, result, CACHE_TTL);
                });
    }

    /**
     * Obtiene todas las categorías activas como un stream reactivo.
     *
     * @return Multi que emite cada categoría individualmente
     */
    @Override
    public Multi<ProductCategoryUomResponse> streamAll() {
        log.info("Streaming all active product category uoms");
        return categoryQueryPort.streamAll()
                .onItem().transform(categoryDtoMapper::toResponse);
    }

    /**
     * Obtiene todas las categorías activas como un stream con filtros.
     *
     * @param filter Filtros a aplicar
     * @return Multi que emite cada categoría que cumple los filtros
     */
    @Override
    public Multi<ProductCategoryUomResponse> streamWithFilter(ProductCategoryUomFilter filter) {
        log.info("Streaming product category uoms with filter: {}", filter);
        return categoryQueryPort.streamWithFilter(filter)
                .onItem().transform(categoryDtoMapper::toResponse);
    }

    // ==================== ListAllProductCategoryUomUseCase ====================

    /**
     * Obtiene todas las categorías que cumplen el filtro sin paginación.
     * Retorna un DTO optimizado para componentes de selección.
     *
     * Implementa cache-aside pattern con TTL de 15 minutos:
     * 1. Intenta obtener del cache
     * 2. Si no existe, consulta la DB
     * 3. Cachea el resultado por 15 minutos
     * 4. Devuelve el resultado
     *
     * El cache se invalida automáticamente en create/update/delete.
     *
     * @param filter Filtros opcionales (por defecto solo categorías activas)
     * @return Uni con lista completa de categorías optimizadas
     */
    @Override
    public Uni<List<ProductCategoryUomSelectResponse>> findAll(ProductCategoryUomFilter filter) {
        log.info("Listing all product category uoms with filter: {}", filter);

        // Generar clave única de cache
        String cacheKey = ProductCategoryUomCacheKeyGenerator.generateKey(filter);

        // Cache-aside pattern
        return categoryCachePort.<ProductCategoryUomSelectResponse>getList(cacheKey)
                .onItem().transformToUni(cachedResult -> {
                    if (cachedResult != null) {
                        log.debug("Returning cached result for key: {}", cacheKey);
                        return Uni.createFrom().item(cachedResult);
                    }

                    // Cache miss - consultar DB
                    log.debug("Cache miss for key: {}. Querying database.", cacheKey);
                    return fetchAllAndCache(filter, cacheKey);
                });
    }

    /**
     * Consulta la DB y cachea el resultado para endpoint /all.
     */
    private Uni<List<ProductCategoryUomSelectResponse>> fetchAllAndCache(
            ProductCategoryUomFilter filter,
            String cacheKey) {

        return categoryQueryPort.findAllWithFilter(filter)
                .onItem().transform(categoryDtoMapper::toSelectResponseList)
                .call(result -> {
                    // Cachear el resultado por 15 minutos (fire-and-forget)
                    log.debug("Caching result for key: {} with TTL of {} minutes",
                            cacheKey, CACHE_ALL_TTL.toMinutes());
                    return categoryCachePort.putList(cacheKey, result, CACHE_ALL_TTL);
                });
    }

    // ==================== GetProductCategoryUomByIdUseCase ====================

    /**
     * Obtiene una categoría por su ID.
     *
     * @throws ProductCategoryUomNotFoundException si no existe una categoría con el ID proporcionado
     */
    @Override
    public Uni<ProductCategoryUom> findById(Integer id) {
        log.info("Getting product category uom by id: {}", id);
        return categoryQueryPort.findById(id)
                .onItem().transform(optional -> optional.orElseThrow(
                        () -> new ProductCategoryUomNotFoundException(id)
                ));
    }

    /**
     * Obtiene una categoría por su código único.
     *
     * @throws ProductCategoryUomNotFoundException si no existe una categoría con el código proporcionado
     */
    @Override
    public Uni<ProductCategoryUom> findByCode(String code) {
        log.info("Getting product category uom by code: {}", code);
        return categoryQueryPort.findByCode(code)
                .onItem().transform(optional -> optional.orElseThrow(
                        () -> new ProductCategoryUomNotFoundException("ProductCategoryUom not found with code: " + code)
                ));
    }

    // ==================== UpdateProductCategoryUomUseCase ====================

    /**
     * Actualiza una categoría existente con nuevos datos.
     */
    @Override
    public Uni<ProductCategoryUom> execute(Integer id, ProductCategoryUom productCategoryUom) {
        log.info("Updating product category uom id: {}", id);

        // Validar unicidad excluyendo el ID actual
        return validateUniqueness(productCategoryUom.getCode(), productCategoryUom.getName(), id)
                .onItem().transformToUni(v -> categoryRepositoryPort.update(productCategoryUom))
                .call(updatedCategory -> {
                    // Invalidar cache después de actualizar
                    log.debug("Invalidating product category uom cache after update");
                    return categoryCachePort.invalidateAll();
                });
    }

    // ==================== DeleteProductCategoryUomUseCase ====================

    /**
     * Elimina lógicamente una categoría (soft delete).
     */
    @Override
    public Uni<Boolean> execute(Integer id) {
        log.info("Soft deleting product category uom id: {}", id);
        return categoryRepositoryPort.softDelete(id)
                .call(deleted -> {
                    if (deleted) {
                        // Invalidar cache después de eliminar
                        log.debug("Invalidating product category uom cache after deletion");
                        return categoryCachePort.invalidateAll();
                    }
                    return Uni.createFrom().voidItem();
                });
    }

    /**
     * Restaura una categoría previamente eliminada.
     */
    @Override
    public Uni<Boolean> restore(Integer id) {
        log.info("Restoring product category uom id: {}", id);
        return categoryRepositoryPort.restore(id)
                .call(restored -> {
                    if (restored) {
                        // Invalidar cache después de restaurar
                        log.debug("Invalidating product category uom cache after restoration");
                        return categoryCachePort.invalidateAll();
                    }
                    return Uni.createFrom().voidItem();
                });
    }

    // ==================== Validaciones ====================

    /**
     * Valida que el código y nombre sean únicos.
     */
    private Uni<Void> validateUniqueness(
            String code,
            String name,
            Integer excludeId) {

        return categoryQueryPort.existsByCode(code, excludeId)
                .onItem().transformToUni(exists -> {
                    if (exists) {
                        return Uni.createFrom().failure(
                                new DuplicateProductCategoryUomException("code", code));
                    }
                    return categoryQueryPort.existsByName(name, excludeId);
                })
                .onItem().transformToUni(exists -> {
                    if (exists) {
                        return Uni.createFrom().failure(
                                new DuplicateProductCategoryUomException("name", name));
                    }
                    return Uni.createFrom().voidItem();
                });
    }
}
