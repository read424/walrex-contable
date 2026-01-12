package org.walrex.domain.service;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.walrex.application.dto.query.PageRequest;
import org.walrex.application.dto.query.ProductAttributeFilter;
import org.walrex.application.dto.response.PagedResponse;
import org.walrex.application.dto.response.ProductAttributeResponse;
import org.walrex.application.dto.response.ProductAttributeSelectResponse;
import org.walrex.application.port.input.*;
import org.walrex.application.port.output.ProductAttributeCachePort;
import org.walrex.application.port.output.ProductAttributeQueryPort;
import org.walrex.application.port.output.ProductAttributeRepositoryPort;
import org.walrex.domain.exception.DuplicateProductAttributeException;
import org.walrex.domain.exception.ProductAttributeNotFoundException;
import org.walrex.domain.model.ProductAttribute;
import org.walrex.infrastructure.adapter.inbound.mapper.ProductAttributeDtoMapper;
import org.walrex.infrastructure.adapter.outbound.cache.ProductAttributeCacheKeyGenerator;
import org.walrex.infrastructure.adapter.outbound.cache.qualifier.ProductAttributeCache;

import java.time.Duration;
import java.util.List;

/**
 * Servicio de dominio para atributos de producto.
 *
 * Implementa todos los casos de uso relacionados con atributos siguiendo
 * el patrón hexagonal. Orquesta la lógica de negocio, validaciones,
 * operaciones de persistencia y caché.
 *
 * IMPORTANTE: Este servicio maneja Integer como tipo de ID (auto-generado).
 */
@Slf4j
@Transactional
@ApplicationScoped
public class ProductAttributeService implements
        CreateProductAttributeUseCase,
        ListProductAttributeUseCase,
        ListAllProductAttributeUseCase,
        GetProductAttributeByIdUseCase,
        UpdateProductAttributeUseCase,
        DeleteProductAttributeUseCase {

    @Inject
    ProductAttributeRepositoryPort attributeRepositoryPort;

    @Inject
    ProductAttributeQueryPort attributeQueryPort;

    @Inject
    @ProductAttributeCache
    ProductAttributeCachePort attributeCachePort;

    @Inject
    ProductAttributeDtoMapper attributeDtoMapper;

    // TTL del cache: 5 minutos para listado paginado
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    // TTL del cache: 15 minutos para listado completo (/all endpoint)
    // Mayor TTL porque los datos son más estáticos y se invalida en cada cambio
    private static final Duration CACHE_ALL_TTL = Duration.ofMinutes(15);

    // ==================== CreateProductAttributeUseCase ====================

    /**
     * Crea un nuevo atributo de producto.
     *
     * @throws DuplicateProductAttributeException si el nombre ya existe
     */
    @Override
    public Uni<ProductAttribute> execute(ProductAttribute productAttribute) {
        log.info("Creating product attribute: {}", productAttribute.getName());

        // Validar unicidad de nombre
        return validateUniqueness(productAttribute.getName(), null)
                .onItem().transformToUni(v -> attributeRepositoryPort.save(productAttribute))
                .call(savedAttribute -> {
                    // Invalidar cache después de crear
                    log.debug("Invalidating product attribute cache after creation");
                    return attributeCachePort.invalidateAll();
                });
    }

    // ==================== ListProductAttributeUseCase ====================

    /**
     * Lista atributos con paginación y filtros.
     *
     * Implementa cache-aside pattern:
     * 1. Intenta obtener del cache
     * 2. Si no existe, consulta la DB
     * 3. Cachea el resultado
     * 4. Devuelve el resultado
     *
     * @param pageRequest Configuración de paginación (page, size, sort)
     * @param filter Filtros opcionales (search, name, displayType, etc.)
     * @return Uni con respuesta paginada
     */
    @Override
    public Uni<PagedResponse<ProductAttributeResponse>> execute(PageRequest pageRequest, ProductAttributeFilter filter) {
        log.info("Listing product attributes with page: {}, size: {}, filter: {}",
                pageRequest.getPage(), pageRequest.getSize(), filter);

        // Generar clave única de cache
        String cacheKey = ProductAttributeCacheKeyGenerator.generateKey(pageRequest, filter);

        // Cache-aside pattern
        return attributeCachePort.get(cacheKey)
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
    private Uni<PagedResponse<ProductAttributeResponse>> fetchFromDatabaseAndCache(
            PageRequest pageRequest,
            ProductAttributeFilter filter,
            String cacheKey) {

        return attributeQueryPort.findAll(pageRequest, filter)
                .onItem().transform(pagedResult -> {
                    var responses = pagedResult.content().stream()
                            .map(attributeDtoMapper::toResponse)
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
                    return attributeCachePort.put(cacheKey, result, CACHE_TTL);
                });
    }

    /**
     * Obtiene todos los atributos activos como un stream reactivo.
     *
     * @return Multi que emite cada atributo individualmente
     */
    @Override
    public Multi<ProductAttributeResponse> streamAll() {
        log.info("Streaming all active product attributes");
        return attributeQueryPort.streamAll()
                .onItem().transform(attributeDtoMapper::toResponse);
    }

    /**
     * Obtiene todos los atributos activos como un stream con filtros.
     *
     * @param filter Filtros a aplicar
     * @return Multi que emite cada atributo que cumple los filtros
     */
    @Override
    public Multi<ProductAttributeResponse> streamWithFilter(ProductAttributeFilter filter) {
        log.info("Streaming product attributes with filter: {}", filter);
        return attributeQueryPort.streamWithFilter(filter)
                .onItem().transform(attributeDtoMapper::toResponse);
    }

    // ==================== ListAllProductAttributeUseCase ====================

    /**
     * Obtiene todos los atributos que cumplen el filtro sin paginación.
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
     * @param filter Filtros opcionales (por defecto solo atributos activos)
     * @return Uni con lista completa de atributos optimizados
     */
    @Override
    public Uni<List<ProductAttributeSelectResponse>> findAll(ProductAttributeFilter filter) {
        log.info("Listing all product attributes with filter: {}", filter);

        // Generar clave única de cache
        String cacheKey = ProductAttributeCacheKeyGenerator.generateKey(filter);

        // Cache-aside pattern
        return attributeCachePort.<ProductAttributeSelectResponse>getList(cacheKey)
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
    private Uni<List<ProductAttributeSelectResponse>> fetchAllAndCache(
            ProductAttributeFilter filter,
            String cacheKey) {

        return attributeQueryPort.findAllWithFilter(filter)
                .onItem().transform(attributeDtoMapper::toSelectResponseList)
                .call(result -> {
                    // Cachear el resultado por 15 minutos (fire-and-forget)
                    log.debug("Caching result for key: {} with TTL of {} minutes",
                            cacheKey, CACHE_ALL_TTL.toMinutes());
                    return attributeCachePort.putList(cacheKey, result, CACHE_ALL_TTL);
                });
    }

    // ==================== GetProductAttributeByIdUseCase ====================

    /**
     * Obtiene un atributo por su ID.
     *
     * @throws ProductAttributeNotFoundException si no existe un atributo con el ID proporcionado
     */
    @Override
    public Uni<ProductAttribute> findById(Integer id) {
        log.info("Getting product attribute by id: {}", id);
        return attributeQueryPort.findById(id)
                .onItem().transform(optional -> optional.orElseThrow(
                        () -> new ProductAttributeNotFoundException("ProductAttribute not found with id: " + id)
                ));
    }

    /**
     * Obtiene un atributo por su nombre.
     *
     * @throws ProductAttributeNotFoundException si no existe un atributo con el nombre proporcionado
     */
    @Override
    public Uni<ProductAttribute> findByName(String name) {
        log.info("Getting product attribute by name: {}", name);
        return attributeQueryPort.findByName(name)
                .onItem().transform(optional -> optional.orElseThrow(
                        () -> new ProductAttributeNotFoundException("ProductAttribute not found with name: " + name)
                ));
    }

    // ==================== UpdateProductAttributeUseCase ====================

    /**
     * Actualiza un atributo existente con nuevos datos.
     */
    @Override
    public Uni<ProductAttribute> execute(Integer id, ProductAttribute productAttribute) {
        log.info("Updating product attribute id: {}", id);

        // Validar unicidad excluyendo el ID actual
        return validateUniqueness(productAttribute.getName(), id)
                .onItem().transformToUni(v -> attributeRepositoryPort.update(productAttribute))
                .call(updatedAttribute -> {
                    // Invalidar cache después de actualizar
                    log.debug("Invalidating product attribute cache after update");
                    return attributeCachePort.invalidateAll();
                });
    }

    // ==================== DeleteProductAttributeUseCase ====================

    /**
     * Elimina lógicamente un atributo (soft delete).
     */
    @Override
    public Uni<Boolean> execute(Integer id) {
        log.info("Soft deleting product attribute id: {}", id);
        return attributeRepositoryPort.softDelete(id)
                .call(deleted -> {
                    if (deleted) {
                        // Invalidar cache después de eliminar
                        log.debug("Invalidating product attribute cache after deletion");
                        return attributeCachePort.invalidateAll();
                    }
                    return Uni.createFrom().voidItem();
                });
    }

    /**
     * Restaura un atributo previamente eliminado.
     */
    @Override
    public Uni<Boolean> restore(Integer id) {
        log.info("Restoring product attribute id: {}", id);
        return attributeRepositoryPort.restore(id)
                .call(restored -> {
                    if (restored) {
                        // Invalidar cache después de restaurar
                        log.debug("Invalidating product attribute cache after restoration");
                        return attributeCachePort.invalidateAll();
                    }
                    return Uni.createFrom().voidItem();
                });
    }

    // ==================== Validaciones ====================

    /**
     * Valida que el nombre sea único.
     *
     * @param name Nombre a validar
     * @param excludeId ID a excluir de la validación (para updates)
     */
    private Uni<Void> validateUniqueness(
            String name,
            Integer excludeId) {

        return attributeQueryPort.existsByName(name, excludeId)
                .onItem().transformToUni(exists -> {
                    if (exists) {
                        return Uni.createFrom().failure(
                                new DuplicateProductAttributeException("name", name));
                    }
                    return Uni.createFrom().voidItem();
                });
    }
}
