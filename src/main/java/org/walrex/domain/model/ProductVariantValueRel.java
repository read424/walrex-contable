package org.walrex.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Modelo de dominio para la relación entre variantes y valores de atributos.
 *
 * Define la combinación de valores de atributos de cada variante.
 * Es una tabla de relación many-to-many.
 *
 * Ejemplo: Variante "Camiseta Roja M" tiene:
 * - Relación 1: variantId=1, valueId=101 (Color: Rojo)
 * - Relación 2: variantId=1, valueId=201 (Talla: M)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariantValueRel {

    /**
     * ID de la variante
     */
    private Integer variantId;

    /**
     * ID del valor del atributo
     */
    private Integer valueId;
}
