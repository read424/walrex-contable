package org.walrex.application.dto.response;

import java.time.OffsetDateTime;

/**
 * DTO de respuesta para un pais individual.
 *
 * Nota: Usamos record (inmutable) para DTOs.
 * No exponemos deletedAt al exterior por seguridad.
 */
public record CountryResponse(
        Integer id,
        String alphabeticCode2,
        String alphabeticCode3,
        Integer numericCode,
        String name,
        String phoneCode,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
){
}
