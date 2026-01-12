package org.walrex.application.dto.response;

import org.walrex.domain.model.AttributeDisplayType;

import java.time.OffsetDateTime;

/**
 * DTO de respuesta para atributos de producto.
 *
 * Contiene todos los campos necesarios para mostrar un atributo en la UI.
 */
public record ProductAttributeResponse(
        Integer id,
        String name,
        AttributeDisplayType displayType,
        Boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
