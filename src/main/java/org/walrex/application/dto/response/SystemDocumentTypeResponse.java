package org.walrex.application.dto.response;

import java.time.OffsetDateTime;

/**
 * DTO de respuesta para un tipo de documento del sistema.
 *
 * Nota: Usamos record (inmutable) para DTOs.
 * No exponemos deletedAt al exterior por seguridad.
 */
public record SystemDocumentTypeResponse(
        Long id,
        String code,
        String name,
        String description,
        Boolean isRequired,
        Boolean forPerson,
        Boolean forCompany,
        Integer priority,
        Boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
