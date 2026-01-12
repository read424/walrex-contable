package org.walrex.domain.strategy;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.walrex.domain.exception.InvalidProductTemplateException;
import org.walrex.domain.model.ProductTemplate;
import org.walrex.domain.model.ProductType;
import org.walrex.domain.model.ProductVariant;

import java.math.BigDecimal;

/**
 * Estrategia para productos de tipo SERVICIO.
 *
 * Reglas de negocio:
 * - NO rastrean inventario
 * - NO tienen propiedades físicas (peso, volumen)
 * - NO usan números de serie
 * - NO tienen gestión de stock
 * - PUEDEN tener allowsPriceEdit=true (edición de precio en venta)
 * - PUEDEN tener variantes (ej: servicio básico, premium, enterprise)
 */
@Slf4j
@ApplicationScoped
public class ServiceProductStrategy implements ProductTypeStrategy {

    @Override
    public void applyTypeSpecificRules(ProductTemplate template) {
        log.info("Applying service-specific rules for product: {}", template.getName());

        // Servicios NO rastrean inventario
        template.setTrackInventory(false);
        template.setUseSerialNumbers(false);

        // Limpiar propiedades físicas
        template.setWeight(null);
        template.setVolume(null);

        // Limpiar gestión de stock
        template.setMinimumStock(null);
        template.setMaximumStock(null);
        template.setReorderPoint(null);
        template.setLeadTime(null);

        log.debug("Service rules applied - inventory tracking disabled, physical properties cleared");
    }

    @Override
    public Uni<Void> validateBusinessRules(ProductTemplate template) {
        log.debug("Validating business rules for service: {}", template.getName());

        // Validar campos obligatorios para servicios
        if (template.getCurrencyId() == null || template.getCurrencyId() <= 0) {
            return Uni.createFrom().failure(
                    new InvalidProductTemplateException(
                            "La moneda es requerida para productos de tipo SERVICE"));
        }

        if (template.getSalePrice() == null) {
            return Uni.createFrom().failure(
                    new InvalidProductTemplateException(
                            "El precio de venta es requerido para productos de tipo SERVICE"));
        }

        if (template.getTaxRate() == null) {
            return Uni.createFrom().failure(
                    new InvalidProductTemplateException(
                            "La tasa de impuesto es requerida para productos de tipo SERVICE"));
        }

        // uomId puede ser 0 o null para servicios (sin unidad de medida específica)
        // cost puede ser 0 para servicios
        // Servicios pueden tener allowsPriceEdit=true (permitido)

        log.debug("Service business rules validated successfully");
        return Uni.createFrom().voidItem();
    }

    @Override
    public boolean supportsVariants() {
        // Servicios SÍ pueden tener variantes
        // Ej: Consultoría (Básica, Estándar, Premium)
        return true;
    }

    @Override
    public boolean requiresDefaultVariant(ProductTemplate template) {
        // Solo si NO tiene variantes configurables
        return Boolean.FALSE.equals(template.getHasVariants());
    }

    @Override
    public Uni<ProductVariant> createDefaultVariant(ProductTemplate template) {
        log.debug("Creating default variant for service: {}", template.getId());

        String sku = template.getInternalReference() != null
                ? template.getInternalReference()
                : "SRV-" + template.getId();

        ProductVariant defaultVariant = ProductVariant.builder()
                .productTemplateId(template.getId())
                .sku(sku)
                .stock(BigDecimal.ZERO)  // Servicios no tienen stock físico
                .priceExtra(BigDecimal.ZERO)
                .costExtra(BigDecimal.ZERO)
                .isDefaultVariant(true)
                .status("active")
                .build();

        return Uni.createFrom().item(defaultVariant);
    }
}
