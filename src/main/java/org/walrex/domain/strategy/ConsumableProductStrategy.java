package org.walrex.domain.strategy;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.walrex.domain.exception.InvalidProductTemplateException;
import org.walrex.domain.model.ProductTemplate;
import org.walrex.domain.model.ProductVariant;

import java.math.BigDecimal;

/**
 * Estrategia para productos CONSUMIBLES (Consumable).
 *
 * Reglas de negocio:
 * - SÍ rastrean inventario
 * - NO usan números de serie (se consumen en masa)
 * - PUEDEN tener propiedades físicas
 * - TIENEN gestión de stock básica
 * - NO pueden tener allowsPriceEdit=true
 * - SÍ pueden tener variantes (ej: diferentes presentaciones)
 */
@Slf4j
@ApplicationScoped
public class ConsumableProductStrategy implements ProductTypeStrategy {

    @Override
    public void applyTypeSpecificRules(ProductTemplate template) {
        log.info("Applying consumable-specific rules for product: {}", template.getName());

        // Consumibles NO usan números de serie
        template.setUseSerialNumbers(false);

        // Rastrear inventario por defecto
        if (template.getTrackInventory() == null) {
            template.setTrackInventory(true);
        }

        log.debug("Consumable rules applied - serial numbers disabled");
    }

    @Override
    public Uni<Void> validateBusinessRules(ProductTemplate template) {
        // Validación: allowsPriceEdit debe ser false para consumibles
        if (Boolean.TRUE.equals(template.getAllowsPriceEdit())) {
            return Uni.createFrom().failure(
                    new InvalidProductTemplateException(
                            "Los productos consumibles no pueden permitir edición de precio durante la venta")
            );
        }

        return Uni.createFrom().voidItem();
    }

    @Override
    public boolean supportsVariants() {
        // Consumibles SÍ pueden tener variantes
        // Ej: Botella de agua (500ml, 1L, 2L)
        return true;
    }

    @Override
    public boolean requiresDefaultVariant(ProductTemplate template) {
        // Solo si NO tiene variantes configurables
        return Boolean.FALSE.equals(template.getHasVariants());
    }

    @Override
    public Uni<ProductVariant> createDefaultVariant(ProductTemplate template) {
        log.debug("Creating default variant for consumable product: {}", template.getId());

        String sku = template.getInternalReference() != null
                ? template.getInternalReference()
                : "CONS-" + template.getId();

        ProductVariant defaultVariant = ProductVariant.builder()
                .productTemplateId(template.getId())
                .sku(sku)
                .stock(BigDecimal.ZERO)  // Stock inicial en 0
                .priceExtra(BigDecimal.ZERO)
                .costExtra(BigDecimal.ZERO)
                .isDefaultVariant(true)
                .status("active")
                .build();

        return Uni.createFrom().item(defaultVariant);
    }
}
