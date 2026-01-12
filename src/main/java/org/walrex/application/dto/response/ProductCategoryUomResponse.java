package org.walrex.application.dto.response;

import java.time.OffsetDateTime;

/**
 * DTO de respuesta para una categoría de unidad de medida individual.
 *
 * Nota: Usamos record (inmutable) para DTOs.
 * No exponemos deletedAt al exterior por seguridad.
 */
public record ProductCategoryUomResponse(
        Integer id,
        String code,
        String name,
        String description,
        boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
