package org.walrex.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Modelo de dominio para plantillas de productos/servicios.
 *
 * Representa una plantilla de producto o servicio que puede ser usado para
 * crear productos específicos, generar cotizaciones, facturas, etc.
 *
 * Incluye validaciones especiales según el tipo de producto:
 * - SERVICE: No permite control de inventario ni propiedades físicas
 * - CONSUMABLE: Permite inventario pero no números de serie
 * - STORABLE: Permite todas las características
 *
 * Soporta soft delete mediante el campo deletedAt.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductTemplate {

    /**
     * Identificador único de la plantilla de producto
     */
    private Integer id;

    /**
     * Nombre del producto o servicio
     * Máximo 255 caracteres, requerido
     */
    private String name;

    /**
     * Referencia interna única del producto
     * Máximo 100 caracteres, opcional pero recomendado
     */
    private String internalReference;

    /**
     * Tipo de producto (storable, consumable, service)
     * Por defecto: STORABLE
     */
    private ProductType type;

    /**
     * ID de la categoría de producto
     * Opcional
     */
    private Integer categoryId;

    /**
     * ID de la marca del producto
     * Opcional
     */
    private Integer brandId;

    /**
     * ID de la unidad de medida
     * Requerido
     */
    private Integer uomId;

    /**
     * ID de la moneda para precios
     * Requerido
     */
    private Integer currencyId;

    /**
     * Precio de venta
     * Por defecto: 0.0
     */
    private BigDecimal salePrice;

    /**
     * Costo del producto
     * Por defecto: 0.0
     */
    private BigDecimal cost;

    /**
     * Indica si el producto está exento de IGV/IVA
     * Por defecto: false
     */
    private Boolean isIGVExempt;

    /**
     * Tasa de impuesto aplicable
     * Por defecto: 0.18 (18%)
     */
    private BigDecimal taxRate;

    /**
     * Peso del producto en la unidad de medida configurada
     * Opcional, solo para productos físicos
     */
    private BigDecimal weight;

    /**
     * Volumen del producto en la unidad de medida configurada
     * Opcional, solo para productos físicos
     */
    private BigDecimal volume;

    /**
     * Indica si se rastrea inventario para este producto
     * Por defecto: true
     * IMPORTANTE: Siempre false para servicios
     */
    private Boolean trackInventory;

    /**
     * Indica si se usan números de serie para este producto
     * Por defecto: false
     * IMPORTANTE: Siempre false para servicios y consumibles
     */
    private Boolean useSerialNumbers;

    /**
     * Stock mínimo recomendado
     * Opcional, solo para productos con control de inventario
     */
    private BigDecimal minimumStock;

    /**
     * Stock máximo recomendado
     * Opcional, solo para productos con control de inventario
     */
    private BigDecimal maximumStock;

    /**
     * Punto de reorden (cuando stock llega a este nivel, se debe reordenar)
     * Opcional, solo para productos con control de inventario
     */
    private BigDecimal reorderPoint;

    /**
     * Tiempo de entrega en días
     * Opcional
     */
    private Integer leadTime;

    /**
     * URL o path de la imagen del producto
     * Opcional
     */
    private String image;

    /**
     * Descripción interna del producto
     * Opcional, texto largo
     */
    private String description;

    /**
     * Descripción para ventas/cotizaciones
     * Opcional, texto largo
     */
    private String descriptionSale;

    /**
     * Código de barras del producto
     * Máximo 100 caracteres, opcional
     */
    private String barcode;

    /**
     * Notas adicionales
     * Opcional, texto largo
     */
    private String notes;

    /**
     * Indica si el producto puede ser vendido
     * Por defecto: true
     */
    private Boolean canBeSold;

    /**
     * Indica si el producto puede ser comprado
     * Por defecto: true
     */
    private Boolean canBePurchased;

    /**
     * Permite editar el precio durante la venta
     * Por defecto: false
     * IMPORTANTE: Solo puede ser true para productos de tipo SERVICE
     */
    private Boolean allowsPriceEdit;

    /**
     * Indica si el producto tiene variantes
     * Por defecto: false
     */
    private Boolean hasVariants;

    /**
     * Estado del producto (active, inactive, discontinued)
     * Por defecto: active
     */
    private String status;

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

    /**
     * Verifica si la plantilla de producto está eliminada (soft delete)
     *
     * @return true si la plantilla ha sido eliminada lógicamente
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * Verifica si la plantilla de producto está activa y no eliminada
     *
     * @return true si la plantilla está activa y no ha sido eliminada
     */
    public boolean isUsable() {
        return "active".equalsIgnoreCase(status) && !isDeleted();
    }

    /**
     * Verifica si este producto es un servicio
     *
     * @return true si el tipo es SERVICE
     */
    public boolean isService() {
        return type == ProductType.SERVICE;
    }
}
