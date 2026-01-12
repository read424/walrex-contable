package org.walrex.application.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * DTO para crear una nueva unidad de medida de producto.
 *
 * Las validaciones aquí son de FORMATO, no de negocio.
 * La validación de unicidad (código) y existencia de categoría se hace en el Service.
 */
public record CreateProductUomRequest(
    /**
     * Código único de la unidad de medida.
     * - Entre 1 y 10 caracteres
     * - Se normalizará a mayúsculas automáticamente
     * - Ej: "M", "KM", "G", "KG", "L", etc.
     */
    @NotBlank(message = "Code is required")
    @Size(min = 1, max = 10, message = "Code must be between 1 and 10 characters")
    String codeUom,

    /**
     * Nombre descriptivo de la unidad de medida.
     * - Entre 2 y 100 caracteres
     */
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    String nameUom,

    /**
     * ID de la categoría a la que pertenece esta unidad de medida.
     * - Obligatorio
     * - Debe existir en product_category_uom
     */
    @NotNull(message = "Category ID is required")
    Integer categoryId,

    /**
     * Factor de conversión a la unidad base de la categoría.
     * - Opcional (por defecto 1.0)
     * - Debe ser mayor que 0
     */
    @DecimalMin(value = "0.000001", message = "Factor must be greater than 0")
    BigDecimal factor,

    /**
     * Precisión de redondeo para cálculos con esta unidad.
     * - Opcional (por defecto 0.01)
     * - Debe ser mayor que 0
     */
    @DecimalMin(value = "0.000001", message = "Rounding precision must be greater than 0")
    BigDecimal roundingPrecision,

    /**
     * Indica si la unidad de medida está activa.
     * - Opcional (por defecto será true si no se especifica)
     */
    Boolean active

) {
    /**
     * Constructor compacto que normaliza los datos.
     */
    public CreateProductUomRequest {
        // Normalizar código a mayúsculas
        if (codeUom != null) {
            codeUom = codeUom.trim().toUpperCase();
        }
        // Normalizar nombre
        if (nameUom != null) {
            nameUom = nameUom.trim();
        }
        // Establecer valores por defecto
        if (factor == null) {
            factor = BigDecimal.ONE;
        }
        if (roundingPrecision == null) {
            roundingPrecision = new BigDecimal("0.01");
        }
        if (active == null) {
            active = true;
        }
    }
}
