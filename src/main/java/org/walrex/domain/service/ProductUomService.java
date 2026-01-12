package org.walrex.domain.service;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.walrex.application.dto.query.PageRequest;
import org.walrex.application.dto.query.ProductUomFilter;
import org.walrex.application.dto.response.PagedResponse;
import org.walrex.application.dto.response.ProductUomResponse;
import org.walrex.application.dto.response.ProductUomSelectResponse;
import org.walrex.application.port.input.*;
import org.walrex.application.port.output.ProductCategoryUomQueryPort;
import org.walrex.application.port.output.ProductUomCachePort;
import org.walrex.application.port.output.ProductUomQueryPort;
import org.walrex.application.port.output.ProductUomRepositoryPort;
import org.walrex.domain.exception.DuplicateProductUomException;
import org.walrex.domain.exception.ProductCategoryUomNotFoundException;
import org.walrex.domain.exception.ProductUomNotFoundException;
import org.walrex.domain.model.ProductUom;
import org.walrex.infrastructure.adapter.inbound.mapper.ProductUomDtoMapper;
import org.walrex.infrastructure.adapter.outbound.cache.ProductUomCacheKeyGenerator;
import org.walrex.infrastructure.adapter.outbound.cache.qualifier.ProductUomCache;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.ProductUomEntity;
import org.walrex.infrastructure.adapter.outbound.persistence.repository.ProductUomRepository;

import java.time.Duration;
import java.util.List;

/**
 * Servicio de dominio para unidades de medida de productos.
 *
 * Implementa todos los casos de uso relacionados con unidades de medida siguiendo
 * el patrón hexagonal. Orquesta la lógica de negocio, validaciones,
 * operaciones de persistencia y caché.
 *
 * IMPORTANTE: Para las respuestas, este servicio:
 * - Carga la entidad con JOIN FETCH para obtener la categoría relacionada
 * - Usa el mapper para convertir Entity → Response (incluye categoryCode y categoryName)
 * - Esto evita N+1 queries y proporciona información completa al frontend
 */
@Slf4j
@Transactional
@ApplicationScoped
public class ProductUomService implements
        CreateProductUomUseCase,
        ListProductUomUseCase,
        ListAllProductUomUseCase,
        GetProductUomByIdUseCase,
        UpdateProductUomUseCase,
        DeleteProductUomUseCase {

    @Inject
    ProductUomRepositoryPort uomRepositoryPort;

    @Inject
    ProductUomQueryPort uomQueryPort;

    @Inject
    ProductCategoryUomQueryPort categoryQueryPort;

    @Inject
    @ProductUomCache
    ProductUomCachePort uomCachePort;

    @Inject
    ProductUomDtoMapper uomDtoMapper;

    @Inject
    ProductUomRepository uomRepository;

    // TTL del cache: 5 minutos para listado paginado
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    // TTL del cache: 15 minutos para listado completo (/all endpoint)
    // Mayor TTL porque los datos son más estáticos y se invalida en cada cambio
    private static final Duration CACHE_ALL_TTL = Duration.ofMinutes(15);

    // ==================== CreateProductUomUseCase ====================

    /**
     * Crea una nueva unidad de medida de producto.
     *
     * Validaciones:
     * - Código único
     * - Categoría existe
     *
     * @throws DuplicateProductUomException si el código ya existe
     * @throws ProductCategoryUomNotFoundException si la categoría no existe
     */
    @Override
    public Uni<ProductUom> execute(ProductUom productUom) {
        log.info("Creating product UOM: {} ({})", productUom.getNameUom(), productUom.getCodeUom());

        // Validar que la categoría existe
        return validateCategoryExists(productUom.getCategoryId())
                .onItem().transformToUni(v ->
                    // Validar unicidad de código
                    validateUniqueness(productUom.getCodeUom(), null)
                )
                .onItem().transformToUni(v -> uomRepositoryPort.save(productUom))
                .call(savedUom -> {
                    // Invalidar cache después de crear
                    log.debug("Invalidating product UOM cache after creation");
                    return uomCachePort.invalidateAll();
                });
    }

    // ==================== ListProductUomUseCase ====================

    /**
     * Lista unidades de medida con paginación y filtros.
     *
     * Implementa cache-aside pattern:
     * 1. Intenta obtener del cache
     * 2. Si no existe, consulta la DB
     * 3. Cachea el resultado
     * 4. Devuelve el resultado
     *
     * IMPORTANTE: Carga las entidades con JOIN FETCH para incluir
     * la información de la categoría en la respuesta.
     *
     * @param pageRequest Configuración de paginación (page, size, sort)
     * @param filter Filtros opcionales (search, code, name, categoryId, etc.)
     * @return Uni con respuesta paginada
     */
    @Override
    public Uni<PagedResponse<ProductUomResponse>> execute(PageRequest pageRequest, ProductUomFilter filter) {
        log.info("Listing product UOMs with page: {}, size: {}, filter: {}",
                pageRequest.getPage(), pageRequest.getSize(), filter);

        // Generar clave única de cache
        String cacheKey = ProductUomCacheKeyGenerator.generateKey(pageRequest, filter);

        // Cache-aside pattern
        return uomCachePort.get(cacheKey)
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
     *
     * IMPORTANTE: Usa el repository directamente para obtener entidades con JOIN FETCH,
     * luego convierte a response con mapper que extrae categoryCode y categoryName.
     */
    private Uni<PagedResponse<ProductUomResponse>> fetchFromDatabaseAndCache(
            PageRequest pageRequest,
            ProductUomFilter filter,
            String cacheKey) {

        // Obtener entidades con JOIN FETCH (incluye category)
        Uni<List<ProductUomEntity>> dataUni = uomRepository.findWithFilters(pageRequest, filter)
                .collect().asList();

        Uni<Long> countUni = uomRepository.countWithFilters(filter);

        // Combinar resultados
        return Uni.combine().all().unis(dataUni, countUni)
                .asTuple()
                .onItem().transform(tuple -> {
                    // Convertir entidades a response (incluye categoryCode y categoryName)
                    List<ProductUomResponse> responses = uomDtoMapper.toResponseListFromEntities(tuple.getItem1());

                    // Convert page from 0-based (backend) to 1-based (frontend)
                    return PagedResponse.of(
                            responses,
                            pageRequest.getPage() + 1,
                            pageRequest.getSize(),
                            tuple.getItem2()
                    );
                })
                .call(result -> {
                    // Cachear el resultado (fire-and-forget)
                    log.debug("Caching result for key: {}", cacheKey);
                    return uomCachePort.put(cacheKey, result, CACHE_TTL);
                });
    }

    /**
     * Obtiene todas las unidades de medida activas como un stream reactivo.
     *
     * @return Multi que emite cada unidad individualmente
     */
    @Override
    public Multi<ProductUomResponse> streamAll() {
        log.info("Streaming all active product UOMs");
        return uomRepository.streamAll()
                .onItem().transform(uomDtoMapper::toResponseFromEntity);
    }

    /**
     * Obtiene todas las unidades de medida activas como un stream con filtros.
     *
     * @param filter Filtros a aplicar
     * @return Multi que emite cada unidad que cumple los filtros
     */
    @Override
    public Multi<ProductUomResponse> streamWithFilter(ProductUomFilter filter) {
        log.info("Streaming product UOMs with filter: {}", filter);
        return uomRepository.findAllWithFilters(filter)
                .onItem().transform(uomDtoMapper::toResponseFromEntity);
    }

    // ==================== ListAllProductUomUseCase ====================

    /**
     * Obtiene todas las unidades de medida que cumplen el filtro sin paginación.
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
     * @param filter Filtros opcionales (por defecto solo unidades activas)
     * @return Uni con lista completa de unidades optimizadas
     */
    @Override
    public Uni<List<ProductUomSelectResponse>> findAll(ProductUomFilter filter) {
        log.info("Listing all product UOMs with filter: {}", filter);

        // Generar clave única de cache
        String cacheKey = ProductUomCacheKeyGenerator.generateKey(filter);

        // Cache-aside pattern
        return uomCachePort.<ProductUomSelectResponse>getList(cacheKey)
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
    private Uni<List<ProductUomSelectResponse>> fetchAllAndCache(
            ProductUomFilter filter,
            String cacheKey) {

        return uomQueryPort.findAllWithFilter(filter)
                .onItem().transform(uomDtoMapper::toSelectResponseList)
                .call(result -> {
                    // Cachear el resultado por 15 minutos (fire-and-forget)
                    log.debug("Caching result for key: {} with TTL of {} minutes",
                            cacheKey, CACHE_ALL_TTL.toMinutes());
                    return uomCachePort.putList(cacheKey, result, CACHE_ALL_TTL);
                });
    }

    // ==================== GetProductUomByIdUseCase ====================

    /**
     * Obtiene una unidad de medida por su ID.
     *
     * @throws ProductUomNotFoundException si no existe una unidad con el ID proporcionado
     */
    @Override
    public Uni<ProductUom> findById(Integer id) {
        log.info("Getting product UOM by id: {}", id);
        return uomQueryPort.findById(id)
                .onItem().transform(optional -> optional.orElseThrow(
                        () -> new ProductUomNotFoundException(id)
                ));
    }

    /**
     * Obtiene una unidad de medida por su código único.
     *
     * @throws ProductUomNotFoundException si no existe una unidad con el código proporcionado
     */
    @Override
    public Uni<ProductUom> findByCode(String code) {
        log.info("Getting product UOM by code: {}", code);
        return uomQueryPort.findByCode(code)
                .onItem().transform(optional -> optional.orElseThrow(
                        () -> new ProductUomNotFoundException("ProductUom not found with code: " + code)
                ));
    }

    // ==================== UpdateProductUomUseCase ====================

    /**
     * Actualiza una unidad de medida existente con nuevos datos.
     *
     * Validaciones:
     * - Código único (excluyendo el propio ID)
     * - Categoría existe
     */
    @Override
    public Uni<ProductUom> execute(Integer id, ProductUom productUom) {
        log.info("Updating product UOM id: {}", id);

        // Validar que la categoría existe
        return validateCategoryExists(productUom.getCategoryId())
                .onItem().transformToUni(v ->
                    // Validar unicidad excluyendo el ID actual
                    validateUniqueness(productUom.getCodeUom(), id)
                )
                .onItem().transformToUni(v -> uomRepositoryPort.update(productUom))
                .call(updatedUom -> {
                    // Invalidar cache después de actualizar
                    log.debug("Invalidating product UOM cache after update");
                    return uomCachePort.invalidateAll();
                });
    }

    // ==================== DeleteProductUomUseCase ====================

    /**
     * Elimina lógicamente una unidad de medida (soft delete).
     */
    @Override
    public Uni<Boolean> execute(Integer id) {
        log.info("Soft deleting product UOM id: {}", id);
        return uomRepositoryPort.softDelete(id)
                .call(deleted -> {
                    if (deleted) {
                        // Invalidar cache después de eliminar
                        log.debug("Invalidating product UOM cache after deletion");
                        return uomCachePort.invalidateAll();
                    }
                    return Uni.createFrom().voidItem();
                });
    }

    /**
     * Restaura una unidad de medida previamente eliminada.
     */
    @Override
    public Uni<Boolean> restore(Integer id) {
        log.info("Restoring product UOM id: {}", id);
        return uomRepositoryPort.restore(id)
                .call(restored -> {
                    if (restored) {
                        // Invalidar cache después de restaurar
                        log.debug("Invalidating product UOM cache after restoration");
                        return uomCachePort.invalidateAll();
                    }
                    return Uni.createFrom().voidItem();
                });
    }

    // ==================== Validaciones ====================

    /**
     * Valida que el código sea único.
     */
    private Uni<Void> validateUniqueness(String code, Integer excludeId) {
        return uomQueryPort.existsByCode(code, excludeId)
                .onItem().transformToUni(exists -> {
                    if (exists) {
                        return Uni.createFrom().failure(
                                new DuplicateProductUomException("code", code));
                    }
                    return Uni.createFrom().voidItem();
                });
    }

    /**
     * Valida que la categoría existe.
     */
    private Uni<Void> validateCategoryExists(Integer categoryId) {
        return categoryQueryPort.findById(categoryId)
                .onItem().transformToUni(optional -> {
                    if (optional.isEmpty()) {
                        return Uni.createFrom().failure(
                                new ProductCategoryUomNotFoundException(categoryId));
                    }
                    return Uni.createFrom().voidItem();
                });
    }
}
