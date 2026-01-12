package org.walrex.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO para actualizar una categoría de unidad de medida existente.
 *
 * Usa las mismas validaciones que CreateProductCategoryUomRequest.
 * En un PUT, todos los campos son obligatorios (reemplazo completo).
 * Para PATCH (actualización parcial) usaríamos campos opcionales.
 */
public record UpdateProductCategoryUomRequest(

        @NotBlank(message = "Code is required")
        @Size(min = 1, max = 20, message = "Code must be between 1 and 20 characters")
        String code,

        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
        String name,

        @Size(max = 255, message = "Description must not exceed 255 characters")
        String description,

        /**
         * Permite activar/desactivar la categoría sin eliminarla.
         */
        Boolean active

) {
    /**
     * Constructor compacto que normaliza los datos.
     */
    public UpdateProductCategoryUomRequest {
        if (code != null) {
            code = code.trim().toUpperCase();
        }
        if (name != null) {
            name = name.trim();
        }
        if (description != null) {
            description = description.trim();
        }
        if (active == null) {
            active = true;
        }
    }
}
