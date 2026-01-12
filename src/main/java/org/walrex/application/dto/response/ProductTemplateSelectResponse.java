package org.walrex.application.dto.response;

import org.walrex.domain.model.ProductType;

import java.math.BigDecimal;

/**
 * DTO de respuesta optimizado para selects/combos de plantillas de producto.
 *
 * Solo incluye campos esenciales para reducir el tamaño de la respuesta.
 */
public record ProductTemplateSelectResponse(
        Integer id,
        String name,
        String internalReference,
        ProductType type,
        BigDecimal salePrice
) {
}
