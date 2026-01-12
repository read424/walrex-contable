package org.walrex.domain.service;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.walrex.application.dto.request.AttributeCombinationRequest;
import org.walrex.application.dto.request.CreateProductTemplateRequest;
import org.walrex.application.dto.request.VariantRequest;
import org.walrex.application.port.input.CreateProductTemplateWithVariantsUseCase;
import org.walrex.application.port.output.ProductAttributeQueryPort;
import org.walrex.application.port.output.ProductAttributeValueQueryPort;
import org.walrex.domain.exception.InvalidProductTemplateException;
import org.walrex.domain.exception.ProductAttributeNotFoundException;
import org.walrex.domain.exception.ProductAttributeValueNotFoundException;
import org.walrex.domain.model.*;
import org.walrex.domain.strategy.ProductTypeStrategy;
import org.walrex.domain.strategy.ProductTypeStrategyFactory;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.ProductTemplateAttributeLineEntity;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.ProductVariantEntity;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.ProductVariantValueRelEntity;
import org.walrex.infrastructure.adapter.outbound.persistence.repository.ProductTemplateAttributeLineRepository;
import org.walrex.infrastructure.adapter.outbound.persistence.repository.ProductVariantValueRelRepository;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio de dominio para crear productos con variantes.
 *
 * Maneja la creación transaccional completa de:
 * - ProductTemplate
 * - ProductTemplateAttributeLine (atributos)
 * - ProductVariant (variantes)
 * - ProductVariantValueRel (combinaciones)
 */
@Slf4j
@Transactional
@ApplicationScoped
public class ProductTemplateWithVariantsService implements CreateProductTemplateWithVariantsUseCase {

    @Inject
    ProductTemplateService productTemplateService;

    @Inject
    ProductAttributeQueryPort attributeQueryPort;

    @Inject
    ProductAttributeValueQueryPort attributeValueQueryPort;

    @Inject
    ProductTemplateAttributeLineRepository attributeLineRepository;

    @Inject
    ProductVariantValueRelRepository variantValueRelRepository;

    @Inject
    ProductTypeStrategyFactory strategyFactory;

    @Override
    public Uni<ProductTemplate> execute(CreateProductTemplateRequest request) {
        log.info("Creating product template with variants: {}", request.name());

        // Validaciones iniciales
        return validateRequest(request)
                // Crear el ProductTemplate base (sin variantes automáticas)
                .flatMap(v -> createProductTemplate(request))
                // Crear las líneas de atributos
                .flatMap(template -> createAttributeLines(template, request.attributeIds())
                        .map(lines -> template))
                // Crear las variantes con sus combinaciones
                .flatMap(template -> createVariantsWithCombinations(template, request.variants())
                        .map(variants -> template))
                .onItem().invoke(template ->
                        log.info("Product template with variants created successfully: id={}, variants={}",
                                template.getId(), request.variants().size())
                );
    }

    /**
     * Valida que el request sea consistente antes de procesar.
     */
    private Uni<Void> validateRequest(CreateProductTemplateRequest request) {
        // Validación 1: Solo STORABLE y CONSUMABLE pueden tener variantes configurables
        if (request.type() == ProductType.SERVICE) {
            return Uni.createFrom().failure(
                    new InvalidProductTemplateException(
                            "Los productos de tipo SERVICE no pueden tener variantes configurables. " +
                            "Use el endpoint estándar para servicios simples."));
        }

        // Validación 2: allowsPriceEdit debe ser false para productos con variantes
        if (Boolean.TRUE.equals(request.allowsPriceEdit())) {
            return Uni.createFrom().failure(
                    new InvalidProductTemplateException(
                            "Los productos con variantes no pueden permitir edición de precio durante la venta"));
        }

        // Validación 3: Debe haber al menos un atributo
        if (request.attributeIds() == null || request.attributeIds().isEmpty()) {
            return Uni.createFrom().failure(
                    new InvalidProductTemplateException("Debe especificar al menos un atributo"));
        }

        // Validación 4: Debe haber al menos una variante
        if (request.variants() == null || request.variants().isEmpty()) {
            return Uni.createFrom().failure(
                    new InvalidProductTemplateException("Debe especificar al menos una variante"));
        }

        // Validación 5: Validar que los atributos existen
        return validateAttributesExist(request.attributeIds())
                // Validación 6: Validar que todos los valores de atributos existen
                .flatMap(v -> validateAttributeValuesExist(request.variants()))
                // Validación 7: Validar que las combinaciones son únicas
                .flatMap(v -> validateUniqueCombinations(request.variants()));
    }

    /**
     * Valida que todos los atributos existen.
     * IMPORTANTE: Ejecuta validaciones secuencialmente para evitar conflictos de Hibernate Reactive.
     */
    private Uni<Void> validateAttributesExist(List<Integer> attributeIds) {
        // Ejecutar validaciones secuencialmente usando Multi
        return Multi.createFrom().iterable(attributeIds)
                .onItem().transformToUniAndConcatenate(attributeId ->
                        attributeQueryPort.findById(attributeId)
                                .onItem().transformToUni(optional -> {
                                    if (optional.isEmpty()) {
                                        return Uni.createFrom().failure(
                                                new ProductAttributeNotFoundException(attributeId.toString()));
                                    }
                                    return Uni.createFrom().voidItem();
                                })
                )
                .toUni()
                .replaceWithVoid();
    }

    /**
     * Valida que todos los valores de atributos usados en variantes existen.
     * IMPORTANTE: Ejecuta validaciones secuencialmente para evitar conflictos de Hibernate Reactive.
     */
    private Uni<Void> validateAttributeValuesExist(List<VariantRequest> variants) {
        // Recolectar todos los valueIds únicos
        Set<Integer> allValueIds = variants.stream()
                .flatMap(variant -> variant.attributeCombination().stream())
                .map(AttributeCombinationRequest::valueId)
                .collect(Collectors.toSet());

        // Ejecutar validaciones secuencialmente usando Multi
        return Multi.createFrom().iterable(allValueIds)
                .onItem().transformToUniAndConcatenate(valueId ->
                        attributeValueQueryPort.findById(valueId)
                                .onItem().transformToUni(optional -> {
                                    if (optional.isEmpty()) {
                                        return Uni.createFrom().failure(
                                                new ProductAttributeValueNotFoundException(valueId));
                                    }
                                    return Uni.createFrom().voidItem();
                                })
                )
                .toUni()
                .replaceWithVoid();
    }

    /**
     * Valida que no haya combinaciones duplicadas de atributos.
     */
    private Uni<Void> validateUniqueCombinations(List<VariantRequest> variants) {
        Set<String> combinations = new HashSet<>();

        for (VariantRequest variant : variants) {
            // Crear una representación única de la combinación (ordenada)
            String combination = variant.attributeCombination().stream()
                    .sorted(Comparator.comparing(AttributeCombinationRequest::attributeId))
                    .map(ac -> ac.attributeId() + ":" + ac.valueId())
                    .collect(Collectors.joining(","));

            if (!combinations.add(combination)) {
                return Uni.createFrom().failure(
                        new InvalidProductTemplateException(
                                "Se encontró una combinación duplicada de atributos en las variantes. " +
                                "Cada variante debe tener una combinación única."));
            }
        }

        return Uni.createFrom().voidItem();
    }

    /**
     * Crea el ProductTemplate base usando el servicio existente.
     */
    private Uni<ProductTemplate> createProductTemplate(CreateProductTemplateRequest request) {
        ProductTemplate template = ProductTemplate.builder()
                .name(request.name())
                .internalReference(request.internalReference())
                .type(request.type())
                .categoryId(request.categoryId())
                .brandId(request.brandId())
                .uomId(request.uomId())
                .currencyId(request.currencyId())
                .salePrice(request.salePrice())
                .cost(request.cost())
                .isIGVExempt(request.isIGVExempt())
                .taxRate(request.taxRate())
                .weight(request.weight())
                .volume(request.volume())
                .trackInventory(request.trackInventory())
                .useSerialNumbers(request.useSerialNumbers())
                .minimumStock(request.minimumStock())
                .maximumStock(request.maximumStock())
                .reorderPoint(request.reorderPoint())
                .leadTime(request.leadTime())
                .image(request.image())
                .description(request.description())
                .descriptionSale(request.descriptionSale())
                .barcode(request.barcode())
                .notes(request.notes())
                .canBeSold(request.canBeSold())
                .canBePurchased(request.canBePurchased())
                .allowsPriceEdit(request.allowsPriceEdit())
                .hasVariants(true)  // IMPORTANTE: Tiene variantes configurables
                .status(request.status())
                .build();

        // Usar el servicio existente que maneja validaciones, reglas de tipo, etc.
        // PERO sin crear variante por defecto (porque crearemos las variantes personalizadas)
        return productTemplateService.execute(template)
                .onItem().invoke(createdTemplate ->
                        log.debug("Base product template created: id={}", createdTemplate.getId())
                );
    }

    /**
     * Crea las líneas de atributos (relación template-attribute).
     * IMPORTANTE: Ejecuta creaciones secuencialmente para evitar conflictos de Hibernate Reactive.
     */
    private Uni<List<ProductTemplateAttributeLineEntity>> createAttributeLines(
            ProductTemplate template,
            List<Integer> attributeIds) {

        log.debug("Creating attribute lines for template id={}, attributes={}", template.getId(), attributeIds);

        // Ejecutar creaciones secuencialmente usando Multi
        return Multi.createFrom().iterable(attributeIds)
                .onItem().transformToUniAndConcatenate(attributeId -> {
                    ProductTemplateAttributeLineEntity line = ProductTemplateAttributeLineEntity.builder()
                            .productTemplateId(template.getId())
                            .attributeId(attributeId)
                            .build();

                    return attributeLineRepository.persist(line);
                })
                .collect().asList()
                .onItem().invoke(lines ->
                        log.debug("Created {} attribute lines", lines.size())
                );
    }

    /**
     * Crea las variantes con sus combinaciones de atributos.
     * IMPORTANTE: Ejecuta creaciones secuencialmente para evitar conflictos de Hibernate Reactive.
     */
    private Uni<List<ProductVariantEntity>> createVariantsWithCombinations(
            ProductTemplate template,
            List<VariantRequest> variantRequests) {

        log.debug("Creating {} variants for template id={}", variantRequests.size(), template.getId());

        // Ejecutar creaciones secuencialmente usando Multi
        return Multi.createFrom().iterable(variantRequests)
                .onItem().transformToUniAndConcatenate(variantRequest ->
                        createSingleVariantWithCombination(template, variantRequest)
                )
                .collect().asList()
                .onItem().invoke(variants ->
                        log.info("Created {} variants with their attribute combinations", variants.size())
                );
    }

    /**
     * Crea una variante individual con su combinación de atributos.
     */
    private Uni<ProductVariantEntity> createSingleVariantWithCombination(
            ProductTemplate template,
            VariantRequest variantRequest) {

        // Crear la variante
        ProductVariantEntity variant = ProductVariantEntity.builder()
                .productTemplateId(template.getId())
                .sku(variantRequest.sku())
                .barcode(variantRequest.barcode())
                .priceExtra(variantRequest.priceExtra() != null ? variantRequest.priceExtra() : BigDecimal.ZERO)
                .costExtra(variantRequest.costExtra() != null ? variantRequest.costExtra() : BigDecimal.ZERO)
                .stock(variantRequest.stock() != null ? variantRequest.stock() : BigDecimal.ZERO)
                .status(variantRequest.status())
                .isDefaultVariant(false)  // No es variante por defecto
                .build();

        // Usar persistAndFlush() para asegurar que el ID se genere inmediatamente
        return variant.persistAndFlush()
                .map(v -> variant)  // persistAndFlush() retorna Uni<Void>, mapeamos a la variante con ID generado
                .flatMap(savedVariant -> {
                    // Crear las relaciones variante-valores secuencialmente
                    return Multi.createFrom().iterable(variantRequest.attributeCombination())
                            .onItem().transformToUniAndConcatenate(combination -> {
                                ProductVariantValueRelEntity rel = ProductVariantValueRelEntity.builder()
                                        .variantId(savedVariant.getId())
                                        .valueId(combination.valueId())
                                        .build();

                                return rel.persist().map(v -> rel);
                            })
                            .collect().asList()
                            .map(relations -> {
                                log.debug("Created variant: sku={}, combinations={}",
                                        savedVariant.getSku(), relations.size());
                                return savedVariant;
                            });
                });
    }
}
