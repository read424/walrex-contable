package org.walrex.infrastructure.adapter.outbound.persistence.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Entidad JPA para unidades de medida de productos.
 *
 * Mapea la tabla 'product_uom' en PostgreSQL.
 * Usa Panache para simplificar operaciones de persistencia reactiva.
 *
 * Configuración:
 * - Extends PanacheEntityBase: Proporciona métodos helper de Panache
 * - @Entity: Marca esta clase como entidad JPA
 * - @Table: Especifica el nombre de la tabla y constraints únicos
 * - @ManyToOne: Relación con ProductCategoryUomEntity
 * - Lombok: Genera getters, setters, constructores, etc.
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@ToString
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "product_uom", uniqueConstraints = {
        @UniqueConstraint(name = "uk_product_uom_code", columnNames = { "code_uom" })
})
public class ProductUomEntity extends PanacheEntityBase {

    /**
     * Identificador único generado automáticamente
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * Código único de la unidad de medida (máximo 10 caracteres)
     */
    @Column(name = "code_uom", nullable = false, unique = true, length = 10)
    private String codeUom;

    /**
     * Nombre descriptivo de la unidad de medida (máximo 100 caracteres)
     */
    @Column(name = "name_uom", nullable = false, length = 100)
    private String nameUom;

    /**
     * ID de la categoría a la que pertenece esta unidad de medida
     */
    @Column(name = "id_category_uom", nullable = false)
    private Integer categoryId;

    /**
     * Relación con la categoría de unidad de medida.
     * IMPORTANTE: Usar JOIN FETCH en queries para evitar N+1 queries.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_category_uom", referencedColumnName = "id", insertable = false, updatable = false)
    private ProductCategoryUomEntity category;

    /**
     * Factor de conversión a la unidad base de la categoría
     * Precisión: 12 dígitos totales, 6 decimales
     * Por defecto: 1.0
     */
    @Column(name = "factor", precision = 12, scale = 6, columnDefinition = "DECIMAL(12, 6) DEFAULT 1.0")
    private BigDecimal factor;

    /**
     * Precisión de redondeo para cálculos con esta unidad
     * Precisión: 12 dígitos totales, 6 decimales
     * Por defecto: 0.01
     */
    @Column(name = "rounding_precision", precision = 12, scale = 6, columnDefinition = "DECIMAL(12, 6) DEFAULT 0.01")
    private BigDecimal roundingPrecision;

    /**
     * Indica si la unidad de medida está activa
     */
    @Column(name = "is_active", columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean active;

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
