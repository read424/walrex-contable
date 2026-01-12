package org.walrex.infrastructure.adapter.outbound.persistence.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.*;
import org.walrex.domain.model.ProductType;
import org.walrex.infrastructure.adapter.outbound.persistence.converter.ProductTypeConverter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Entidad JPA para plantillas de producto/servicio.
 *
 * Mapea la tabla 'product_templates' en PostgreSQL.
 * Usa Panache para simplificar operaciones de persistencia reactiva.
 *
 * Configuración:
 * - Extends PanacheEntityBase: Proporciona métodos helper de Panache
 * - @Entity: Marca esta clase como entidad JPA
 * - @Table: Especifica el nombre de la tabla y constraints únicos
 * - @ManyToOne: Relaciones con CategoryProductEntity, ProductBrandEntity, ProductUomEntity, CurrencyEntity
 * - @Convert: Usa ProductTypeConverter para el enum ProductType
 * - Lombok: Genera getters, setters, constructores, etc.
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@ToString
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "product_templates", uniqueConstraints = {
        @UniqueConstraint(name = "uk_product_template_internal_ref", columnNames = { "internal_reference" })
})
public class ProductTemplateEntity extends PanacheEntityBase {

    /**
     * Identificador único generado automáticamente
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * Nombre del producto o servicio (máximo 255 caracteres)
     */
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    /**
     * Referencia interna única del producto (máximo 100 caracteres)
     */
    @Column(name = "internal_reference", unique = true, length = 100)
    private String internalReference;

    /**
     * Tipo de producto (storable, consumable, service)
     * Usa ProductTypeConverter para conversión de enum a string
     */
    @Column(name = "type_product", nullable = false, length = 20)
    @Convert(converter = ProductTypeConverter.class)
    private ProductType type;

    /**
     * ID de la categoría de producto
     */
    @Column(name = "id_category")
    private Integer categoryId;

    /**
     * Relación con la categoría de producto.
     * IMPORTANTE: Usar JOIN FETCH en queries para evitar N+1 queries.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_category", referencedColumnName = "id", insertable = false, updatable = false)
    private CategoryProductEntity category;

    /**
     * ID de la marca del producto
     */
    @Column(name = "id_brand")
    private Integer brandId;

    /**
     * Relación con la marca del producto.
     * IMPORTANTE: Usar JOIN FETCH en queries para evitar N+1 queries.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_brand", referencedColumnName = "id", insertable = false, updatable = false)
    private ProductBrandEntity brand;

    /**
     * ID de la unidad de medida
     */
    @Column(name = "id_uom", nullable = false)
    private Integer uomId;

    /**
     * Relación con la unidad de medida.
     * IMPORTANTE: Usar JOIN FETCH en queries para evitar N+1 queries.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_uom", referencedColumnName = "id", insertable = false, updatable = false)
    private ProductUomEntity uom;

    /**
     * ID de la moneda
     */
    @Column(name = "id_currency", nullable = false)
    private Integer currencyId;

    /**
     * Relación con la moneda.
     * IMPORTANTE: Usar JOIN FETCH en queries para evitar N+1 queries.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_currency", referencedColumnName = "id", insertable = false, updatable = false)
    private CurrencyEntity currency;

    /**
     * Precio de venta
     * Precisión: 12 dígitos totales, 4 decimales
     */
    @Column(name = "sale_price", precision = 12, scale = 4, columnDefinition = "DECIMAL(12, 4) DEFAULT 0.0")
    private BigDecimal salePrice;

    /**
     * Costo del producto
     * Precisión: 12 dígitos totales, 4 decimales
     */
    @Column(name = "cost", precision = 12, scale = 4, columnDefinition = "DECIMAL(12, 4) DEFAULT 0.0")
    private BigDecimal cost;

    /**
     * Indica si el producto está exento de IGV/IVA
     */
    @Column(name = "is_igv_exempt", columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean isIGVExempt;

    /**
     * Tasa de impuesto aplicable
     * Precisión: 5 dígitos totales, 4 decimales (0.0000 a 1.0000)
     */
    @Column(name = "tax_rate", precision = 5, scale = 4, columnDefinition = "DECIMAL(5, 4) DEFAULT 0.18")
    private BigDecimal taxRate;

    /**
     * Peso del producto
     * Precisión: 10 dígitos totales, 3 decimales
     */
    @Column(name = "weight", precision = 10, scale = 3)
    private BigDecimal weight;

    /**
     * Volumen del producto
     * Precisión: 10 dígitos totales, 3 decimales
     */
    @Column(name = "volume", precision = 10, scale = 3)
    private BigDecimal volume;

    /**
     * Indica si se rastrea inventario para este producto
     */
    @Column(name = "track_inventory", columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean trackInventory;

    /**
     * Indica si se usan números de serie
     */
    @Column(name = "use_serial_numbers", columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean useSerialNumbers;

    /**
     * Stock mínimo recomendado
     * Precisión: 12 dígitos totales, 2 decimales
     */
    @Column(name = "minimum_stock", precision = 12, scale = 2)
    private BigDecimal minimumStock;

    /**
     * Stock máximo recomendado
     * Precisión: 12 dígitos totales, 2 decimales
     */
    @Column(name = "maximum_stock", precision = 12, scale = 2)
    private BigDecimal maximumStock;

    /**
     * Punto de reorden
     * Precisión: 12 dígitos totales, 2 decimales
     */
    @Column(name = "reorder_point", precision = 12, scale = 2)
    private BigDecimal reorderPoint;

    /**
     * Tiempo de entrega en días
     */
    @Column(name = "lead_time")
    private Integer leadTime;

    /**
     * URL o path de la imagen del producto
     */
    @Column(name = "image", columnDefinition = "TEXT")
    private String image;

    /**
     * Descripción interna del producto
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Descripción para ventas/cotizaciones
     */
    @Column(name = "description_sale", columnDefinition = "TEXT")
    private String descriptionSale;

    /**
     * Código de barras del producto
     */
    @Column(name = "barcode", length = 100)
    private String barcode;

    /**
     * Notas adicionales
     */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /**
     * Indica si el producto puede ser vendido
     */
    @Column(name = "can_be_sold", columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean canBeSold;

    /**
     * Indica si el producto puede ser comprado
     */
    @Column(name = "can_be_purchased", columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean canBePurchased;

    /**
     * Permite editar el precio durante la venta
     * Solo puede ser true para productos de tipo SERVICE
     */
    @Column(name = "allows_price_edit", columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean allowsPriceEdit;

    /**
     * Indica si el producto tiene variantes
     */
    @Column(name = "has_variants", columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean hasVariants;

    /**
     * Estado del producto (active, inactive, discontinued)
     */
    @Column(name = "status", length = 20, columnDefinition = "VARCHAR(20) DEFAULT 'active'")
    private String status;

    /**
     * Fecha de creación del registro
     */
    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    /**
     * Fecha de última actualización del registro
     */
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    /**
     * Fecha de eliminación lógica del registro
     * Si es null, el registro no ha sido eliminado
     */
    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;
}
