package org.walrex.application.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO para crear una variante de producto con combinación de atributos.
 *
 * Ejemplo: Camiseta Roja M
 * - sku: "CAM-001-ROJO-M"
 * - priceExtra: 0.00 (mismo precio que el template)
 * - attributeCombination: [{attributeId: 10, valueId: 101}, {attributeId: 11, valueId: 201}]
 */
public record VariantRequest(
    /**
     * SKU único de la variante
     */
    @Size(min = 1, max = 25, message = "El SKU debe tener entre 1 y 25 caracteres")
    String sku,

    /**
     * Código de barras opcional
     */
    @Size(max = 100, message = "El código de barras debe tener máximo 100 caracteres")
    String barcode,

    /**
     * Precio extra sobre el precio base del template
     */
    @DecimalMin(value = "0.0", inclusive = true, message = "El precio extra debe ser mayor o igual a 0")
    BigDecimal priceExtra,

    /**
     * Costo extra sobre el costo base del template
     */
    @DecimalMin(value = "0.0", inclusive = true, message = "El costo extra debe ser mayor o igual a 0")
    BigDecimal costExtra,

    /**
     * Stock inicial de esta variante
     */
    @DecimalMin(value = "0.0", inclusive = true, message = "El stock debe ser mayor o igual a 0")
    BigDecimal stock,

    /**
     * Estado de la variante (active, inactive, discontinued)
     */
    @Size(max = 20, message = "El estado debe tener máximo 20 caracteres")
    String status,

    /**
     * Combinación única de valores de atributos
     * Ejemplo: [{attributeId: 10, valueId: 101}, {attributeId: 11, valueId: 201}]
     */
    @NotEmpty(message = "La combinación de atributos es requerida")
    @Valid
    List<AttributeCombinationRequest> attributeCombination
) {
    /**
     * Constructor compacto que normaliza los datos.
     */
    public VariantRequest {
        if (sku != null) {
            sku = sku.trim().toUpperCase();
        }
        if (barcode != null) {
            barcode = barcode.trim();
        }
        if (priceExtra == null) {
            priceExtra = BigDecimal.ZERO;
        }
        if (costExtra == null) {
            costExtra = BigDecimal.ZERO;
        }
        if (stock == null) {
            stock = BigDecimal.ZERO;
        }
        if (status == null || status.isBlank()) {
            status = "active";
        } else {
            status = status.trim().toLowerCase();
        }
    }
}
