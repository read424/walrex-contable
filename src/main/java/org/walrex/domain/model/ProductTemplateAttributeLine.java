package org.walrex.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Modelo de dominio para líneas de atributos de plantillas de producto.
 *
 * Representa la relación entre un ProductTemplate y un ProductAttribute.
 * Define qué atributos están disponibles para un producto.
 *
 * Ejemplo: Producto "Camiseta" tiene atributos "Talla" y "Color"
 * - Línea 1: templateId=1, attributeId=10 (Talla)
 * - Línea 2: templateId=1, attributeId=11 (Color)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductTemplateAttributeLine {

    /**
     * Identificador único de la línea de atributo
     */
    private Integer id;

    /**
     * ID de la plantilla de producto
     */
    private Integer productTemplateId;

    /**
     * ID del atributo asignado
     */
    private Integer attributeId;

    /**
     * Fecha de creación del registro
     */
    private OffsetDateTime createdAt;

    /**
     * Fecha de última actualización del registro
     */
    private OffsetDateTime updatedAt;
}
