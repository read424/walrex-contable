package org.walrex.application.dto.response;

import java.time.OffsetDateTime;

/**
 * DTO de respuesta para una moneda individual.
 *
 * Nota: Usamos record (inmutable) para DTOs.
 * No exponemos deletedAt al exterior por seguridad.
 */
public record CurrencyResponse(
        Integer id,
        String alphabeticCode,
        String numericCode,
        String name,
        boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
