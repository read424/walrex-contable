package org.walrex.domain.strategy;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.walrex.domain.exception.InvalidProductTemplateException;
import org.walrex.domain.model.ProductTemplate;
import org.walrex.domain.model.ProductVariant;

import java.math.BigDecimal;

/**
 * Estrategia para productos ALMACENABLES (Storable).
 *
 * Reglas de negocio:
 * - SÍ rastrean inventario por defecto
 * - PUEDEN usar números de serie
 * - TIENEN propiedades físicas (peso, volumen)
 * - TIENEN gestión de stock (min, max, reorder point)
 * - NO pueden tener allowsPriceEdit=true
 * - SÍ pueden tener variantes (ej: diferentes tallas, colores)
 */
@Slf4j
@ApplicationScoped
public class StorableProductStrategy implements ProductTypeStrategy {

    @Override
    public void applyTypeSpecificRules(ProductTemplate template) {
        log.info("Applying storable-specific rules for product: {}", template.getName());

        // Productos almacenables: El usuario controla todos los campos
        // No se fuerzan cambios automáticos

        // Por defecto, trackInventory debería ser true (ya viene del DTO)
        if (template.getTrackInventory() == null) {
            template.setTrackInventory(true);
        }

        log.debug("Storable rules applied - no automatic field modifications");
    }

    @Override
    public Uni<Void> validateBusinessRules(ProductTemplate template) {
        // Validación: allowsPriceEdit debe ser false para almacenables
        if (Boolean.TRUE.equals(template.getAllowsPriceEdit())) {
            return Uni.createFrom().failure(
                    new InvalidProductTemplateException(
                            "Los productos almacenables no pueden permitir edición de precio durante la venta")
            );
        }

        // Validación opcional: Si rastrea inventario, debería tener UOM apropiada
        // (Esta validación podría agregarse según reglas de negocio)

        return Uni.createFrom().voidItem();
    }

    @Override
    public boolean supportsVariants() {
        // Almacenables SÍ pueden tener variantes
        // Es el caso de uso principal (tallas, colores, etc.)
        return true;
    }

    @Override
    public boolean requiresDefaultVariant(ProductTemplate template) {
        // Solo si NO tiene variantes configurables
        return Boolean.FALSE.equals(template.getHasVariants());
    }

    @Override
    public Uni<ProductVariant> createDefaultVariant(ProductTemplate template) {
        log.debug("Creating default variant for storable product: {}", template.getId());

        String sku = template.getInternalReference() != null
                ? template.getInternalReference()
                : "PRD-" + template.getId();

        ProductVariant defaultVariant = ProductVariant.builder()
                .productTemplateId(template.getId())
                .sku(sku)
                .stock(BigDecimal.ZERO)  // Stock inicial en 0, se actualiza con movimientos
                .priceExtra(BigDecimal.ZERO)
                .costExtra(BigDecimal.ZERO)
                .isDefaultVariant(true)
                .status("active")
                .build();

        return Uni.createFrom().item(defaultVariant);
    }
}
