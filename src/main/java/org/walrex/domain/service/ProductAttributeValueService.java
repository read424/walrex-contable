package org.walrex.domain.service;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.walrex.application.dto.query.PageRequest;
import org.walrex.application.dto.query.ProductAttributeValueFilter;
import org.walrex.application.dto.response.PagedResponse;
import org.walrex.application.dto.response.ProductAttributeValueResponse;
import org.walrex.application.dto.response.ProductAttributeValueSelectResponse;
import org.walrex.application.port.input.*;
import org.walrex.application.port.output.ProductAttributeCachePort;
import org.walrex.application.port.output.ProductAttributeQueryPort;
import org.walrex.application.port.output.ProductAttributeValueCachePort;
import org.walrex.application.port.output.ProductAttributeValueQueryPort;
import org.walrex.application.port.output.ProductAttributeValueRepositoryPort;
import org.walrex.domain.exception.DuplicateProductAttributeValueException;
import org.walrex.domain.exception.ProductAttributeNotFoundException;
import org.walrex.domain.exception.ProductAttributeValueNotFoundException;
import org.walrex.domain.model.ProductAttributeValue;
import org.walrex.infrastructure.adapter.inbound.mapper.ProductAttributeValueDtoMapper;
import org.walrex.infrastructure.adapter.outbound.cache.ProductAttributeValueCacheKeyGenerator;
import org.walrex.infrastructure.adapter.outbound.cache.qualifier.ProductAttributeValueCache;

import java.time.Duration;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Servicio de dominio para valores de atributos de producto.
 *
 * Implementa todos los casos de uso relacionados con valores de atributos siguiendo
 * el patrón hexagonal. Orquesta la lógica de negocio, validaciones,
 * operaciones de persistencia y caché.
 *
 * IMPORTANTE: Este servicio maneja Integer como tipo de ID (auto-generado).
 */
@Slf4j
@Transactional
@ApplicationScoped
public class ProductAttributeValueService implements
        CreateProductAttributeValueUseCase,
        ListProductAttributeValueUseCase,
        ListAllProductAttributeValueUseCase,
        GetProductAttributeValueByIdUseCase,
        UpdateProductAttributeValueUseCase,
        DeleteProductAttributeValueUseCase {

    @Inject
    ProductAttributeValueRepositoryPort valueRepositoryPort;

    @Inject
    ProductAttributeValueQueryPort valueQueryPort;

    @Inject
    ProductAttributeQueryPort attributeQueryPort;

    @Inject
    @ProductAttributeValueCache
    ProductAttributeValueCachePort valueCachePort;

    @Inject
    ProductAttributeValueDtoMapper valueDtoMapper;

    // TTL del cache: 5 minutos para listado paginado
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    // TTL del cache: 15 minutos para listado completo (/all endpoint)
    // Mayor TTL porque los datos son más estáticos y se invalida en cada cambio
    private static final Duration CACHE_ALL_TTL = Duration.ofMinutes(15);

    // Patrón para validar HTML color: #RRGGBB
    private static final Pattern HTML_COLOR_PATTERN = Pattern.compile("^#[0-9A-Fa-f]{6}$");

    // ==================== CreateProductAttributeValueUseCase ====================

    /**
     * Crea un nuevo valor de atributo de producto.
     *
     * @throws DuplicateProductAttributeValueException si (attributeId+name) ya existe
     * @throws ProductAttributeNotFoundException si el attributeId no existe
     */
    @Override
    public Uni<ProductAttributeValue> execute(ProductAttributeValue productAttributeValue) {
        log.info("Creating product attribute value: {}", productAttributeValue.getName());

        // Validar que el atributo existe
        return validateAttributeExists(productAttributeValue.getAttributeId())
                .onItem().transformToUni(v ->
                    // Validar unicidad de (attributeId, name)
                    validateUniqueness(null,
                            productAttributeValue.getAttributeId(),
                            productAttributeValue.getName(),
                            null)
                )
                .onItem().transformToUni(v ->
                    // Validar formato del htmlColor si se proporciona
                    validateHtmlColor(productAttributeValue.getHtmlColor())
                )
                .onItem().transformToUni(v -> valueRepositoryPort.save(productAttributeValue))
                .call(savedValue -> {
                    // Invalidar cache después de crear
                    log.debug("Invalidating product attribute value cache after creation");
                    return valueCachePort.invalidateAll();
                });
    }

    // ==================== ListProductAttributeValueUseCase ====================

    /**
     * Lista valores de atributos con paginación y filtros.
     *
     * Implementa cache-aside pattern:
     * 1. Intenta obtener del cache
     * 2. Si no existe, consulta la DB
     * 3. Cachea el resultado
     * 4. Devuelve el resultado
     *
     * @param pageRequest Configuración de paginación (page, size, sort)
     * @param filter Filtros opcionales (search, attributeId, active, etc.)
     * @return Uni con respuesta paginada
     */
    @Override
    public Uni<PagedResponse<ProductAttributeValueResponse>> execute(PageRequest pageRequest, ProductAttributeValueFilter filter) {
        log.info("Listing product attribute values with page: {}, size: {}, filter: {}",
                pageRequest.getPage(), pageRequest.getSize(), filter);

        // Generar clave única de cache
        String cacheKey = ProductAttributeValueCacheKeyGenerator.generateKey(pageRequest, filter);

        // Cache-aside pattern
        return valueCachePort.get(cacheKey)
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
    private Uni<PagedResponse<ProductAttributeValueResponse>> fetchFromDatabaseAndCache(
            PageRequest pageRequest,
            ProductAttributeValueFilter filter,
            String cacheKey) {

        return valueQueryPort.findAll(pageRequest, filter)
                .onItem().transform(pagedResult -> {
                    var responses = pagedResult.content().stream()
                            .map(valueDtoMapper::toResponse)
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
                    return valueCachePort.put(cacheKey, result, CACHE_TTL);
                });
    }

    /**
     * Obtiene todos los valores de atributos activos como un stream reactivo.
     *
     * @return Multi que emite cada valor de atributo individualmente
     */
    @Override
    public Multi<ProductAttributeValueResponse> streamAll() {
        log.info("Streaming all active product attribute values");
        return valueQueryPort.streamAll()
                .onItem().transform(valueDtoMapper::toResponse);
    }

    /**
     * Obtiene todos los valores de atributos activos como un stream con filtros.
     *
     * @param filter Filtros a aplicar
     * @return Multi que emite cada valor de atributo que cumple los filtros
     */
    @Override
    public Multi<ProductAttributeValueResponse> streamWithFilter(ProductAttributeValueFilter filter) {
        log.info("Streaming product attribute values with filter: {}", filter);
        return valueQueryPort.streamWithFilter(filter)
                .onItem().transform(valueDtoMapper::toResponse);
    }

    // ==================== ListAllProductAttributeValueUseCase ====================

    /**
     * Obtiene todos los valores de atributos que cumplen el filtro sin paginación.
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
     * @param filter Filtros opcionales (por defecto solo valores activos)
     * @return Uni con lista completa de valores optimizados
     */
    @Override
    public Uni<List<ProductAttributeValueSelectResponse>> findAll(ProductAttributeValueFilter filter) {
        log.info("Listing all product attribute values with filter: {}", filter);

        // Generar clave única de cache
        String cacheKey = ProductAttributeValueCacheKeyGenerator.generateKey(filter);

        // Cache-aside pattern
        return valueCachePort.<ProductAttributeValueSelectResponse>getList(cacheKey)
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
    private Uni<List<ProductAttributeValueSelectResponse>> fetchAllAndCache(
            ProductAttributeValueFilter filter,
            String cacheKey) {

        return valueQueryPort.findAllWithFilter(filter)
                .onItem().transform(valueDtoMapper::toSelectResponseList)
                .call(result -> {
                    // Cachear el resultado por 15 minutos (fire-and-forget)
                    log.debug("Caching result for key: {} with TTL of {} minutes",
                            cacheKey, CACHE_ALL_TTL.toMinutes());
                    return valueCachePort.putList(cacheKey, result, CACHE_ALL_TTL);
                });
    }

    // ==================== GetProductAttributeValueByIdUseCase ====================

    /**
     * Obtiene un valor de atributo por su ID.
     *
     * @throws ProductAttributeValueNotFoundException si no existe un valor con el ID proporcionado
     */
    @Override
    public Uni<ProductAttributeValue> findById(Integer id) {
        log.info("Getting product attribute value by id: {}", id);
        return valueQueryPort.findById(id)
                .onItem().transform(optional -> optional.orElseThrow(
                        () -> new ProductAttributeValueNotFoundException(id)
                ));
    }

    /**
     * Obtiene un valor de atributo por su nombre.
     *
     * @throws ProductAttributeValueNotFoundException si no existe un valor con el nombre proporcionado
     */
    @Override
    public Uni<ProductAttributeValue> findByName(String name) {
        log.info("Getting product attribute value by name: {}", name);
        return valueQueryPort.findByName(name)
                .onItem().transform(optional -> optional.orElseThrow(
                        () -> new ProductAttributeValueNotFoundException("ProductAttributeValue not found with name: " + name, true)
                ));
    }

    // ==================== UpdateProductAttributeValueUseCase ====================

    /**
     * Actualiza un valor de atributo existente con nuevos datos.
     */
    @Override
    public Uni<ProductAttributeValue> execute(Integer id, ProductAttributeValue productAttributeValue) {
        log.info("Updating product attribute value id: {}", id);

        // Validar unicidad excluyendo el ID actual
        return validateUniqueness(null,
                        productAttributeValue.getAttributeId(),
                        productAttributeValue.getName(),
                        id)
                .onItem().transformToUni(v ->
                    // Validar formato del htmlColor si se proporciona
                    validateHtmlColor(productAttributeValue.getHtmlColor())
                )
                .onItem().transformToUni(v -> valueRepositoryPort.update(productAttributeValue))
                .call(updatedValue -> {
                    // Invalidar cache después de actualizar
                    log.debug("Invalidating product attribute value cache after update");
                    return valueCachePort.invalidateAll();
                });
    }

    // ==================== DeleteProductAttributeValueUseCase ====================

    /**
     * Elimina lógicamente un valor de atributo (soft delete).
     */
    @Override
    public Uni<Boolean> execute(Integer id) {
        log.info("Soft deleting product attribute value id: {}", id);
        return valueRepositoryPort.softDelete(id)
                .call(deleted -> {
                    if (deleted) {
                        // Invalidar cache después de eliminar
                        log.debug("Invalidating product attribute value cache after deletion");
                        return valueCachePort.invalidateAll();
                    }
                    return Uni.createFrom().voidItem();
                });
    }

    /**
     * Restaura un valor de atributo previamente eliminado.
     */
    @Override
    public Uni<Boolean> restore(Integer id) {
        log.info("Restoring product attribute value id: {}", id);
        return valueRepositoryPort.restore(id)
                .call(restored -> {
                    if (restored) {
                        // Invalidar cache después de restaurar
                        log.debug("Invalidating product attribute value cache after restoration");
                        return valueCachePort.invalidateAll();
                    }
                    return Uni.createFrom().voidItem();
                });
    }

    // ==================== Validaciones ====================

    /**
     * Valida que el atributo existe.
     */
    private Uni<Void> validateAttributeExists(Integer attributeId) {
        return attributeQueryPort.findById(attributeId)
                .onItem().transformToUni(optional -> {
                    if (optional.isEmpty()) {
                        return Uni.createFrom().failure(
                                new ProductAttributeNotFoundException(attributeId.toString()));
                    }
                    return Uni.createFrom().voidItem();
                });
    }

    /**
     * Valida que la combinación (attributeId, name) sea única.
     *
     * @param id ID a validar (null para skip, ya no se valida porque es auto-generado)
     * @param attributeId ID del atributo
     * @param name Nombre a validar
     * @param excludeId ID a excluir de la validación (para updates)
     */
    private Uni<Void> validateUniqueness(
            Integer id,
            Integer attributeId,
            String name,
            Integer excludeId) {

        // Solo validar unicidad de (attributeId, name)
        // El ID ya no se valida porque es auto-generado
        return valueQueryPort.existsByAttributeIdAndName(attributeId, name, excludeId)
                .onItem().transformToUni(exists -> {
                    if (exists) {
                        return Uni.createFrom().failure(
                                DuplicateProductAttributeValueException.withAttributeIdAndName(attributeId, name));
                    }
                    return Uni.createFrom().voidItem();
                });
    }

    /**
     * Valida el formato del htmlColor si se proporciona.
     *
     * Formato requerido: #RRGGBB (6 dígitos hexadecimales)
     */
    private Uni<Void> validateHtmlColor(String htmlColor) {
        if (htmlColor == null || htmlColor.isBlank()) {
            // El color es opcional, null es válido
            return Uni.createFrom().voidItem();
        }

        String trimmedColor = htmlColor.trim();

        if (!HTML_COLOR_PATTERN.matcher(trimmedColor).matches()) {
            return Uni.createFrom().failure(
                    new IllegalArgumentException(
                            "Invalid HTML color format: '" + htmlColor + "'. " +
                            "Must be in format #RRGGBB (e.g., #FF0000, #00FF00)")
            );
        }

        return Uni.createFrom().voidItem();
    }
}
