package org.walrex.infrastructure.adapter.outbound.persistence.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Entidad JPA para variantes de producto.
 *
 * Mapea la tabla 'product_variants' en PostgreSQL.
 * Usa Panache para simplificar operaciones de persistencia reactiva.
 *
 * Las variantes son un complemento interno de ProductTemplate y no se exponen
 * directamente al API. Se crean automáticamente cuando ProductTemplate.hasVariants
 * es true.
 *
 * Configuración:
 * - Extends PanacheEntityBase: Proporciona métodos helper de Panache
 * - @Entity: Marca esta clase como entidad JPA
 * - @Table: Especifica el nombre de la tabla y constraints únicos
 * - @ManyToOne: Relación con ProductTemplateEntity
 * - Lombok: Genera getters, setters, constructores, etc.
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@ToString
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "product_variants", uniqueConstraints = {
        @UniqueConstraint(name = "uk_product_variant_sku", columnNames = { "sku" })
})
public class ProductVariantEntity extends PanacheEntityBase {

    /**
     * Identificador único generado automáticamente
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * ID de la plantilla de producto a la que pertenece
     */
    @Column(name = "product_template_id", nullable = false)
    private Integer productTemplateId;

    /**
     * Relación con la plantilla de producto.
     * IMPORTANTE: Usar con LAZY loading para evitar N+1 queries.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_template_id", referencedColumnName = "id", insertable = false, updatable = false)
    private ProductTemplateEntity productTemplate;

    /**
     * SKU (Stock Keeping Unit) único de la variante
     * Máximo 25 caracteres, único
     */
    @Column(name = "sku", unique = true, length = 25)
    private String sku;

    /**
     * Código de barras de la variante
     * Máximo 100 caracteres, opcional
     */
    @Column(name = "barcode", length = 100)
    private String barcode;

    /**
     * Precio adicional respecto al precio base
     * Precisión: 12 dígitos totales, 4 decimales
     */
    @Column(name = "price_extra", precision = 12, scale = 4, columnDefinition = "DECIMAL(12, 4) DEFAULT 0.0")
    private BigDecimal priceExtra;

    /**
     * Costo adicional respecto al costo base
     * Precisión: 12 dígitos totales, 4 decimales
     */
    @Column(name = "cost_extra", precision = 12, scale = 4, columnDefinition = "DECIMAL(12, 4) DEFAULT 0.0")
    private BigDecimal costExtra;

    /**
     * Stock disponible para esta variante
     * Precisión: 12 dígitos totales, 2 decimales
     */
    @Column(name = "stock", precision = 12, scale = 2, columnDefinition = "DECIMAL(12, 2) DEFAULT 0.0")
    private BigDecimal stock;

    /**
     * Estado de la variante (active, inactive)
     */
    @Column(name = "status", length = 20, columnDefinition = "VARCHAR(20) DEFAULT 'active'")
    private String status;

    /**
     * Indica si es la variante por defecto de la plantilla
     */
    @Column(name = "is_default_variant", columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean isDefaultVariant;

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

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
