package org.walrex.application.dto.response;

import org.walrex.domain.model.ProductType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * DTO de respuesta para una plantilla de producto individual.
 *
 * Incluye información de las entidades relacionadas (categoryName, brandName, uomCode, currencyCode)
 * para evitar consultas adicionales en el frontend.
 *
 * Nota: Usamos record (inmutable) para DTOs.
 * No exponemos deletedAt al exterior por seguridad.
 */
public record ProductTemplateResponse(
        Integer id,
        String name,
        String internalReference,
        ProductType type,
        Integer categoryId,
        String categoryName,
        Integer brandId,
        String brandName,
        Integer uomId,
        String uomCode,
        Integer currencyId,
        String currencyCode,
        BigDecimal salePrice,
        BigDecimal cost,
        Boolean isIGVExempt,
        BigDecimal taxRate,
        BigDecimal weight,
        BigDecimal volume,
        Boolean trackInventory,
        Boolean useSerialNumbers,
        BigDecimal minimumStock,
        BigDecimal maximumStock,
        BigDecimal reorderPoint,
        Integer leadTime,
        String image,
        String description,
        String descriptionSale,
        String barcode,
        String notes,
        Boolean canBeSold,
        Boolean canBePurchased,
        Boolean allowsPriceEdit,
        Boolean hasVariants,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
