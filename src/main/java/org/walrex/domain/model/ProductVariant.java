package org.walrex.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Modelo de dominio para variantes de productos.
 *
 * Representa una variante específica de una plantilla de producto, utilizada
 * internamente para crear variantes de productos cuando se habilita la opción
 * de variantes en ProductTemplate.
 *
 * Las variantes no se exponen directamente al API, son gestionadas internamente
 * por ProductTemplateService al crear productos con variantes.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariant {

    /**
     * Identificador único de la variante
     */
    private Integer id;

    /**
     * ID de la plantilla de producto a la que pertenece
     */
    private Integer productTemplateId;

    /**
     * SKU (Stock Keeping Unit) único de la variante
     * Máximo 25 caracteres, único
     */
    private String sku;

    /**
     * Código de barras de la variante
     * Máximo 100 caracteres, opcional
     */
    private String barcode;

    /**
     * Precio adicional respecto al precio base de la plantilla
     * Precisión: 12 dígitos totales, 4 decimales
     * Por defecto: 0.0
     */
    private BigDecimal priceExtra;

    /**
     * Costo adicional respecto al costo base de la plantilla
     * Precisión: 12 dígitos totales, 4 decimales
     * Por defecto: 0.0
     */
    private BigDecimal costExtra;

    /**
     * Stock disponible para esta variante
     * Precisión: 12 dígitos totales, 2 decimales
     * Por defecto: 0.0
     */
    private BigDecimal stock;

    /**
     * Estado de la variante (active, inactive)
     * Por defecto: active
     */
    private String status;

    /**
     * Indica si es la variante por defecto de la plantilla
     * Por defecto: false
     */
    private Boolean isDefaultVariant;

    /**
     * Fecha de creación del registro
     */
    private OffsetDateTime createdAt;

    /**
     * Fecha de última actualización del registro
     */
    private OffsetDateTime updatedAt;

    /**
     * Fecha de eliminación lógica del registro
     * Si es null, el registro no ha sido eliminado
     */
    private OffsetDateTime deletedAt;
}
