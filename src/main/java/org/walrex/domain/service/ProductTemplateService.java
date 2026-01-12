package org.walrex.domain.service;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.walrex.application.dto.query.PageRequest;
import org.walrex.application.dto.query.ProductTemplateFilter;
import org.walrex.application.dto.response.PagedResponse;
import org.walrex.application.dto.response.ProductTemplateResponse;
import org.walrex.application.dto.response.ProductTemplateSelectResponse;
import org.walrex.application.port.input.*;
import org.walrex.application.port.output.*;
import org.walrex.domain.exception.*;
import org.walrex.domain.model.ProductTemplate;
import org.walrex.domain.model.ProductType;
import org.walrex.domain.model.ProductVariant;
import org.walrex.domain.strategy.ProductTypeStrategy;
import org.walrex.domain.strategy.ProductTypeStrategyFactory;
import org.walrex.infrastructure.adapter.inbound.mapper.ProductTemplateDtoMapper;
import org.walrex.infrastructure.adapter.outbound.cache.ProductTemplateCacheKeyGenerator;
import org.walrex.infrastructure.adapter.outbound.cache.qualifier.ProductTemplateCache;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.ProductTemplateEntity;
import org.walrex.infrastructure.adapter.outbound.persistence.repository.ProductTemplateRepository;
import org.walrex.infrastructure.adapter.outbound.persistence.repository.ProductBrandRepository;
import org.walrex.infrastructure.adapter.outbound.persistence.repository.CategoryProductRepository;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Servicio de dominio para plantillas de producto.
 *
 * Implementa todos los casos de uso relacionados con plantillas de producto siguiendo
 * el patrón hexagonal. Orquesta la lógica de negocio, validaciones,
 * operaciones de persistencia y caché.
 *
 * IMPORTANTE: Este servicio aplica reglas de validación específicas por tipo de producto:
 * - SERVICE: Desactiva control de inventario y limpia propiedades físicas
 * - CONSUMABLE: Desactiva números de serie
 * - STORABLE: Sin cambios automáticos, usuario controla todo
 */
@Slf4j
@Transactional
@ApplicationScoped
public class ProductTemplateService implements
        CreateProductTemplateUseCase,
        ListProductTemplateUseCase,
        ListAllProductTemplateUseCase,
        GetProductTemplateByIdUseCase,
        UpdateProductTemplateUseCase,
        DeleteProductTemplateUseCase {

    @Inject
    ProductTemplateRepositoryPort templateRepositoryPort;

    @Inject
    ProductTemplateQueryPort templateQueryPort;

    @Inject
    ProductUomQueryPort uomQueryPort;

    @Inject
    CurrencyQueryPort currencyQueryPort;

    @Inject
    @ProductTemplateCache
    ProductTemplateCachePort templateCachePort;

    @Inject
    ProductTemplateDtoMapper templateDtoMapper;

    @Inject
    ProductTemplateRepository templateRepository;

    @Inject
    ProductBrandRepository brandRepository;

    @Inject
    CategoryProductRepository categoryRepository;

    @Inject
    ProductVariantRepositoryPort variantRepositoryPort;

    @Inject
    ProductTypeStrategyFactory strategyFactory;

    // TTL del cache: 5 minutos para listado paginado
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    // TTL del cache: 15 minutos para listado completo (/all endpoint)
    private static final Duration CACHE_ALL_TTL = Duration.ofMinutes(15);

    // ==================== CreateProductTemplateUseCase ====================

    /**
     * Crea una nueva plantilla de producto.
     *
     * Validaciones:
     * - Unicidad de referencia interna (si se proporciona)
     * - Existencia de categoría (si se proporciona)
     * - Existencia de marca (si se proporciona)
     * - Existencia de unidad de medida (requerido)
     * - Existencia de moneda (requerido)
     * - Aplicación de reglas específicas por tipo de producto
     *
     * @throws DuplicateProductTemplateException si la referencia interna ya existe
     * @throws ProductUomNotFoundException si la UOM no existe
     * @throws CurrencyNotFoundException si la moneda no existe
     * @throws InvalidProductTemplateException si categoría o marca no existen
     */
    @Override
    public Uni<ProductTemplate> execute(ProductTemplate productTemplate) {
        log.info("Creating product template: {} ({})", productTemplate.getName(), productTemplate.getInternalReference());

        // Validar unicidad de referencia interna (si se proporciona)
        Uni<Void> uniquenessValidation = Uni.createFrom().voidItem();
        if (productTemplate.getInternalReference() != null && !productTemplate.getInternalReference().isBlank()) {
            uniquenessValidation = validateUniqueness(productTemplate.getInternalReference(), null);
        }

        return uniquenessValidation
                // Validar que la categoría existe (si se proporciona)
                .onItem().transformToUni(v -> {
                    if (productTemplate.getCategoryId() != null) {
                        return validateCategoryExists(productTemplate.getCategoryId());
                    }
                    return Uni.createFrom().voidItem();
                })
                // Validar que la marca existe (si se proporciona)
                .onItem().transformToUni(v -> {
                    if (productTemplate.getBrandId() != null) {
                        return validateBrandExists(productTemplate.getBrandId());
                    }
                    return Uni.createFrom().voidItem();
                })
                // Validar que la UOM existe (si se proporciona y es válido)
                // Para SERVICE puede ser null o 0
                .onItem().transformToUni(v -> {
                    if (productTemplate.getUomId() != null && productTemplate.getUomId() > 0) {
                        return validateUomExists(productTemplate.getUomId());
                    }
                    return Uni.createFrom().voidItem();
                })
                // Validar que la moneda existe (si se proporciona y es válido)
                .onItem().transformToUni(v -> {
                    if (productTemplate.getCurrencyId() != null && productTemplate.getCurrencyId() > 0) {
                        return validateCurrencyExists(productTemplate.getCurrencyId());
                    }
                    return Uni.createFrom().voidItem();
                })
                // Validar que allowsPriceEdit solo puede ser true para servicios
                .onItem().transformToUni(v -> validateAllowsPriceEdit(productTemplate))
                // Aplicar reglas específicas por tipo de producto
                .onItem().invoke(() -> applyTypeSpecificRules(productTemplate))
                // Guardar en base de datos
                .onItem().transformToUni(v -> templateRepositoryPort.save(productTemplate))
                // Crear variante por defecto si no tiene variantes configurables
                .call(savedTemplate -> {
                    if (Boolean.FALSE.equals(savedTemplate.getHasVariants())) {
                        log.info("Creating default variant for product template id: {}", savedTemplate.getId());
                        return createDefaultVariant(savedTemplate);
                    }
                    return Uni.createFrom().voidItem();
                })
                // Invalidar cache
                .call(savedTemplate -> {
                    log.debug("Invalidating product template cache after creation");
                    return templateCachePort.invalidateAll();
                });
    }

    // ==================== ListProductTemplateUseCase ====================

    /**
     * Lista plantillas de producto con paginación y filtros.
     *
     * Implementa cache-aside pattern para mejorar rendimiento.
     */
    @Override
    public Uni<PagedResponse<ProductTemplateResponse>> execute(PageRequest pageRequest, ProductTemplateFilter filter) {
        log.info("Listing product templates with page: {}, size: {}, filter: {}",
                pageRequest.getPage(), pageRequest.getSize(), filter);

        // Generar clave única de cache
        String cacheKey = ProductTemplateCacheKeyGenerator.generateKey(pageRequest, filter);

        // Cache-aside pattern
        return templateCachePort.get(cacheKey)
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
     * Usa SQL nativo con LEFT OUTER JOIN para evitar problemas de Hibernate Reactive.
     */
    private Uni<PagedResponse<ProductTemplateResponse>> fetchFromDatabaseAndCache(
            PageRequest pageRequest,
            ProductTemplateFilter filter,
            String cacheKey) {

        // Usar query SQL nativa con LEFT OUTER JOIN
        Uni<List<Map<String, Object>>> dataUni = templateRepository.findWithFiltersNative(pageRequest, filter);
        Uni<Long> countUni = templateRepository.countWithFiltersNative(filter);

        // Combinar resultados
        return Uni.combine().all().unis(dataUni, countUni)
                .asTuple()
                .onItem().transform(tuple -> {
                    // Convertir Maps a response
                    List<ProductTemplateResponse> responses = templateDtoMapper.toResponseListFromMaps(tuple.getItem1());

                    // Page is already 0-based (standard REST API convention)
                    return PagedResponse.of(
                            responses,
                            pageRequest.getPage(),
                            pageRequest.getSize(),
                            tuple.getItem2()
                    );
                })
                .call(result -> {
                    // Cachear el resultado (fire-and-forget)
                    log.debug("Caching result for key: {}", cacheKey);
                    return templateCachePort.put(cacheKey, result, CACHE_TTL);
                });
    }

    @Override
    public Multi<ProductTemplateResponse> streamAll() {
        log.info("Streaming all active product templates");
        return templateRepository.streamAll()
                .onItem().transform(templateDtoMapper::toResponseFromEntity);
    }

    @Override
    public Multi<ProductTemplateResponse> streamWithFilter(ProductTemplateFilter filter) {
        log.info("Streaming product templates with filter: {}", filter);
        return templateRepository.findAllWithFilters(filter)
                .onItem().transform(templateDtoMapper::toResponseFromEntity);
    }

    // ==================== ListAllProductTemplateUseCase ====================

    /**
     * Obtiene todas las plantillas de producto que cumplen el filtro sin paginación.
     * Implementa cache-aside pattern con TTL de 15 minutos.
     */
    @Override
    public Uni<List<ProductTemplateSelectResponse>> findAll(ProductTemplateFilter filter) {
        log.info("Listing all product templates with filter: {}", filter);

        // Generar clave única de cache
        String cacheKey = ProductTemplateCacheKeyGenerator.generateKey(filter);

        // Cache-aside pattern
        return templateCachePort.<ProductTemplateSelectResponse>getList(cacheKey)
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
    private Uni<List<ProductTemplateSelectResponse>> fetchAllAndCache(
            ProductTemplateFilter filter,
            String cacheKey) {

        return templateQueryPort.findAllWithFilter(filter)
                .onItem().transform(templateDtoMapper::toSelectResponseList)
                .call(result -> {
                    // Cachear el resultado por 15 minutos (fire-and-forget)
                    log.debug("Caching result for key: {} with TTL of {} minutes",
                            cacheKey, CACHE_ALL_TTL.toMinutes());
                    return templateCachePort.putList(cacheKey, result, CACHE_ALL_TTL);
                });
    }

    // ==================== GetProductTemplateByIdUseCase ====================

    @Override
    public Uni<ProductTemplate> findById(Integer id) {
        log.info("Getting product template by id: {}", id);
        return templateQueryPort.findById(id)
                .onItem().transform(optional -> optional.orElseThrow(
                        () -> new ProductTemplateNotFoundException(id)
                ));
    }

    @Override
    public Uni<ProductTemplate> findByInternalReference(String internalReference) {
        log.info("Getting product template by internal reference: {}", internalReference);
        return templateQueryPort.findByInternalReference(internalReference)
                .onItem().transform(optional -> optional.orElseThrow(
                        () -> new ProductTemplateNotFoundException("Plantilla de producto no encontrada con referencia: " + internalReference)
                ));
    }

    // ==================== UpdateProductTemplateUseCase ====================

    /**
     * Actualiza una plantilla de producto existente.
     *
     * Validaciones:
     * - Unicidad de referencia interna excluyendo el propio ID
     * - Existencia de categoría (si se proporciona)
     * - Existencia de marca (si se proporciona)
     * - Existencia de unidad de medida (requerido)
     * - Existencia de moneda (requerido)
     * - Aplicación de reglas específicas por tipo de producto
     */
    @Override
    public Uni<ProductTemplate> execute(Integer id, ProductTemplate productTemplate) {
        log.info("Updating product template id: {}", id);

        productTemplate.setId(id);

        // Validar unicidad de referencia interna (excluyendo el propio ID)
        Uni<Void> uniquenessValidation = Uni.createFrom().voidItem();
        if (productTemplate.getInternalReference() != null && !productTemplate.getInternalReference().isBlank()) {
            uniquenessValidation = validateUniqueness(productTemplate.getInternalReference(), id);
        }

        return uniquenessValidation
                // Validar que la categoría existe (si se proporciona)
                .onItem().transformToUni(v -> {
                    if (productTemplate.getCategoryId() != null) {
                        return validateCategoryExists(productTemplate.getCategoryId());
                    }
                    return Uni.createFrom().voidItem();
                })
                // Validar que la marca existe (si se proporciona)
                .onItem().transformToUni(v -> {
                    if (productTemplate.getBrandId() != null) {
                        return validateBrandExists(productTemplate.getBrandId());
                    }
                    return Uni.createFrom().voidItem();
                })
                // Validar que la UOM existe (si se proporciona y es válido)
                // Para SERVICE puede ser null o 0
                .onItem().transformToUni(v -> {
                    if (productTemplate.getUomId() != null && productTemplate.getUomId() > 0) {
                        return validateUomExists(productTemplate.getUomId());
                    }
                    return Uni.createFrom().voidItem();
                })
                // Validar que la moneda existe (si se proporciona y es válido)
                .onItem().transformToUni(v -> {
                    if (productTemplate.getCurrencyId() != null && productTemplate.getCurrencyId() > 0) {
                        return validateCurrencyExists(productTemplate.getCurrencyId());
                    }
                    return Uni.createFrom().voidItem();
                })
                // Validar que allowsPriceEdit solo puede ser true para servicios
                .onItem().transformToUni(v -> validateAllowsPriceEdit(productTemplate))
                // Aplicar reglas específicas por tipo de producto
                .onItem().invoke(() -> applyTypeSpecificRules(productTemplate))
                // Actualizar en base de datos
                .onItem().transformToUni(v -> templateRepositoryPort.update(productTemplate))
                // Invalidar cache
                .call(updatedTemplate -> {
                    log.debug("Invalidating product template cache after update");
                    return templateCachePort.invalidateAll();
                });
    }

    // ==================== DeleteProductTemplateUseCase ====================

    @Override
    public Uni<Boolean> execute(Integer id) {
        log.info("Soft deleting product template id: {}", id);
        return templateRepositoryPort.softDelete(id)
                .call(deleted -> {
                    if (deleted) {
                        log.debug("Invalidating product template cache after deletion");
                        return templateCachePort.invalidateAll();
                    }
                    return Uni.createFrom().voidItem();
                });
    }

    @Override
    public Uni<Boolean> restore(Integer id) {
        log.info("Restoring product template id: {}", id);
        return templateRepositoryPort.restore(id)
                .call(restored -> {
                    if (restored) {
                        log.debug("Invalidating product template cache after restoration");
                        return templateCachePort.invalidateAll();
                    }
                    return Uni.createFrom().voidItem();
                });
    }

    // ==================== Validaciones ====================

    /**
     * Valida que la referencia interna sea única.
     */
    private Uni<Void> validateUniqueness(String internalReference, Integer excludeId) {
        return templateQueryPort.existsByInternalReference(internalReference, excludeId)
                .onItem().transformToUni(exists -> {
                    if (exists) {
                        return Uni.createFrom().failure(
                                new DuplicateProductTemplateException(internalReference));
                    }
                    return Uni.createFrom().voidItem();
                });
    }

    /**
     * Valida que la categoría existe.
     */
    private Uni<Void> validateCategoryExists(Integer categoryId) {
        return categoryRepository.count("id = ?1", categoryId)
                .onItem().transformToUni(count -> {
                    if (count == 0) {
                        return Uni.createFrom().failure(
                                new InvalidProductTemplateException("La categoría especificada no existe: " + categoryId));
                    }
                    return Uni.createFrom().voidItem();
                });
    }

    /**
     * Valida que la marca existe.
     */
    private Uni<Void> validateBrandExists(Integer brandId) {
        return brandRepository.count("id = ?1", brandId)
                .onItem().transformToUni(count -> {
                    if (count == 0) {
                        return Uni.createFrom().failure(
                                new InvalidProductTemplateException("La marca especificada no existe: " + brandId));
                    }
                    return Uni.createFrom().voidItem();
                });
    }

    /**
     * Valida que la unidad de medida existe.
     */
    private Uni<Void> validateUomExists(Integer uomId) {
        return uomQueryPort.findById(uomId)
                .onItem().transformToUni(optional -> {
                    if (optional.isEmpty()) {
                        return Uni.createFrom().failure(
                                new ProductUomNotFoundException(uomId));
                    }
                    return Uni.createFrom().voidItem();
                });
    }

    /**
     * Valida que la moneda existe.
     */
    private Uni<Void> validateCurrencyExists(Integer currencyId) {
        return currencyQueryPort.findById(currencyId)
                .onItem().transformToUni(optional -> {
                    if (optional.isEmpty()) {
                        return Uni.createFrom().failure(
                                new CurrencyNotFoundException(currencyId));
                    }
                    return Uni.createFrom().voidItem();
                });
    }

    /**
     * Valida que allowsPriceEdit solo puede ser true para productos de tipo SERVICE.
     *
     * Regla de negocio:
     * - Por defecto: false para todos los tipos
     * - Solo puede ser true si type = SERVICE
     * - Si true y type != SERVICE → error de validación
     */
    private Uni<Void> validateAllowsPriceEdit(ProductTemplate productTemplate) {
        if (Boolean.TRUE.equals(productTemplate.getAllowsPriceEdit())
                && productTemplate.getType() != ProductType.SERVICE) {
            return Uni.createFrom().failure(
                    new InvalidProductTemplateException(
                        "Solo los productos de tipo SERVICIO pueden permitir edición de precio durante la venta"));
        }
        return Uni.createFrom().voidItem();
    }

    /**
     * Aplica reglas específicas por tipo de producto.
     *
     * CRÍTICO: Esta es la lógica central de validación por tipo.
     *
     * - SERVICE: Desactiva control de inventario y limpia propiedades físicas
     * - CONSUMABLE: Desactiva números de serie
     * - STORABLE: Sin cambios automáticos
     */
    /**
     * Aplica reglas de negocio específicas del tipo de producto usando Strategy Pattern.
     *
     * Delega la lógica específica a la estrategia correspondiente en vez de usar condicionales.
     * Esto facilita agregar nuevos tipos de producto sin modificar este método.
     */
    private void applyTypeSpecificRules(ProductTemplate productTemplate) {
        ProductTypeStrategy strategy = strategyFactory.getStrategy(productTemplate.getType());
        strategy.applyTypeSpecificRules(productTemplate);
    }

    /**
     * Crea una variante por defecto usando Strategy Pattern.
     *
     * Delega la creación a la estrategia correspondiente, que conoce
     * los detalles específicos de cada tipo de producto.
     *
     * @param template Plantilla de producto guardada con ID
     * @return Uni<ProductVariant> Variante creada
     */
    private Uni<ProductVariant> createDefaultVariant(ProductTemplate template) {
        ProductTypeStrategy strategy = strategyFactory.getStrategy(template.getType());

        return strategy.createDefaultVariant(template)
                .flatMap(variant -> variantRepositoryPort.save(variant))
                .onItem().invoke(variant ->
                        log.info("Default variant created: id={}, sku={} for template id={}",
                                variant.getId(), variant.getSku(), template.getId())
                );
    }
}
