package org.walrex.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO para crear una nueva categoría de unidad de medida.
 *
 * Las validaciones aquí son de FORMATO, no de negocio.
 * La validación de unicidad (código) se hace en el Service.
 */
public record CreateProductCategoryUomRequest(
    /**
     * Código único de la categoría.
     * - Máximo 20 caracteres
     * - Formato libre (sin restricciones de patrón)
     * - Ej: "LENGTH", "WEIGHT", "VOLUME", etc.
     */
    @NotBlank(message = "Code is required")
    @Size(min = 1, max = 20, message = "Code must be between 1 and 20 characters")
    String code,

    /**
     * Nombre descriptivo de la categoría.
     * - Entre 2 y 100 caracteres
     */
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    String name,

    /**
     * Descripción detallada de la categoría.
     * - Opcional
     * - Máximo 255 caracteres
     */
    @Size(max = 255, message = "Description must not exceed 255 characters")
    String description,

    /**
     * Indica si la categoría está activa.
     * - Opcional (por defecto será true si no se especifica)
     */
    Boolean active

) {
    /**
     * Constructor compacto que normaliza los datos.
     */
    public CreateProductCategoryUomRequest {
        if (code != null) {
            code = code.trim().toUpperCase();
        }
        if (name != null) {
            name = name.trim();
        }
        if (description != null) {
            description = description.trim();
        }
        // Si active es null, establecer true como valor por defecto
        if (active == null) {
            active = true;
        }
    }
}
