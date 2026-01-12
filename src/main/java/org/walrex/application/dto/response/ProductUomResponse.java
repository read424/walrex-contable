package org.walrex.application.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * DTO de respuesta para una unidad de medida de producto individual.
 *
 * Incluye información de la categoría relacionada (categoryCode, categoryName)
 * para evitar consultas adicionales en el frontend.
 *
 * Nota: Usamos record (inmutable) para DTOs.
 * No exponemos deletedAt al exterior por seguridad.
 */
public record ProductUomResponse(
        Integer id,
        String codeUom,
        String nameUom,
        Integer categoryId,
        String categoryCode,
        String categoryName,
        BigDecimal factor,
        BigDecimal roundingPrecision,
        boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
