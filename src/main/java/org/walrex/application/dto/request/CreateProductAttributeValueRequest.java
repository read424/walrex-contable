package org.walrex.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO para crear un nuevo valor de atributo de producto.
 *
 * Las validaciones aquí son de FORMATO, no de negocio.
 * La validación de unicidad (attributeId+name) se hace en el Service.
 * El ID es auto-generado por la base de datos y no se incluye en el request.
 */
public record CreateProductAttributeValueRequest(
    /**
     * ID del atributo al que pertenece este valor.
     * - Requerido
     * - Debe existir en product_attributes
     */
    @NotNull(message = "Attribute ID is required")
    Integer attributeId,

    /**
     * Nombre descriptivo del valor.
     * - Entre 2 y 100 caracteres
     */
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    String name,

    /**
     * Color HTML asociado al valor (opcional).
     * - Formato: #RRGGBB (6 dígitos hexadecimales)
     * - Solo se usa cuando el attributeDisplayType es COLOR
     */
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "HTML color must be in format #RRGGBB")
    String htmlColor,

    /**
     * Secuencia para ordenamiento (opcional).
     * - Por defecto: 0
     */
    Integer sequence,

    /**
     * Indica si el valor está activo.
     * - Opcional (por defecto será true si no se especifica)
     */
    Boolean active

) {
    /**
     * Constructor compacto que normaliza los datos.
     */
    public CreateProductAttributeValueRequest {
        // Normalizar nombre: trim
        if (name != null) {
            name = name.trim();
        }
        // Normalizar htmlColor: uppercase y trim
        if (htmlColor != null && !htmlColor.isBlank()) {
            htmlColor = htmlColor.trim().toUpperCase();
        }
        // Si sequence es null, establecer 0 como valor por defecto
        if (sequence == null) {
            sequence = 0;
        }
        // Si active es null, establecer true como valor por defecto
        if (active == null) {
            active = true;
        }
    }
}
