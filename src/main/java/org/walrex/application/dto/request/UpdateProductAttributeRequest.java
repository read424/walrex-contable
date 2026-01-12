package org.walrex.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.walrex.domain.model.AttributeDisplayType;

/**
 * DTO para actualizar un atributo de producto existente.
 *
 * NOTA: El ID no se incluye en este request porque viene en el path parameter.
 * El ID no puede ser modificado una vez creado el atributo.
 */
public record UpdateProductAttributeRequest(
    /**
     * Nombre descriptivo del atributo.
     * - Entre 2 y 100 caracteres
     */
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    String name,

    /**
     * Tipo de visualización del atributo.
     * - Requerido
     * - Valores válidos: SELECT, RADIO, COLOR, TEXT
     */
    @NotNull(message = "Display type is required")
    AttributeDisplayType displayType,

    /**
     * Indica si el atributo está activo.
     * - Opcional (por defecto será true si no se especifica)
     */
    Boolean active

) {
    /**
     * Constructor compacto que normaliza los datos.
     */
    public UpdateProductAttributeRequest {
        // Normalizar nombre: trim
        if (name != null) {
            name = name.trim();
        }
        // Si active es null, establecer true como valor por defecto
        if (active == null) {
            active = true;
        }
    }
}
