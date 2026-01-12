package org.walrex.application.dto.response;

import org.walrex.domain.model.AttributeDisplayType;

import java.time.OffsetDateTime;

/**
 * DTO de respuesta para un valor de atributo de producto individual.
 *
 * Incluye información del atributo relacionado (attributeName, attributeDisplayType)
 * para evitar consultas adicionales en el frontend.
 *
 * Nota: Usamos record (inmutable) para DTOs.
 * No exponemos deletedAt al exterior por seguridad.
 */
public record ProductAttributeValueResponse(
        Integer id,
        Integer attributeId,
        String attributeName,
        AttributeDisplayType attributeDisplayType,
        String name,
        String htmlColor,
        Integer sequence,
        boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
