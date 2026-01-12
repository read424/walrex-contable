package org.walrex.infrastructure.adapter.outbound.persistence.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * Entidad JPA para valores de atributos de producto.
 *
 * Mapea la tabla 'product_attribute_values' en PostgreSQL.
 * Usa Panache para simplificar operaciones de persistencia reactiva.
 *
 * IMPORTANTE: El ID es de tipo Integer (auto-generado por la base de datos).
 *
 * RELACIÓN: Tiene una relación @ManyToOne con ProductAttributeEntity.
 * La combinación (attributeId, name) debe ser única (constraint uk_attribute_value).
 *
 * Configuración:
 * - Extends PanacheEntityBase: Proporciona métodos helper de Panache
 * - @Entity: Marca esta clase como entidad JPA
 * - @Table: Especifica el nombre de la tabla y constraints únicos
 * - Lombok: Genera getters, setters, constructores, etc.
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@ToString(exclude = "attribute") // Evitar lazy loading en toString
@EqualsAndHashCode(callSuper = true, exclude = "attribute") // Evitar lazy loading en equals/hashCode
@Entity
@Table(name = "product_attribute_values", uniqueConstraints = {
        @UniqueConstraint(name = "uk_attribute_value", columnNames = {"attribute_id", "name"})
})
public class ProductAttributeValueEntity extends PanacheEntityBase {

    /**
     * Identificador único del valor de atributo (Integer auto-generado)
     * Auto-incrementado por la base de datos
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    /**
     * ID del atributo al que pertenece este valor
     * Referencia a ProductAttributeEntity.id
     */
    @Column(name = "attribute_id", nullable = false)
    private Integer attributeId;

    /**
     * Relación con el atributo de producto.
     * IMPORTANTE: Usar JOIN FETCH en queries para evitar N+1 queries.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attribute_id", referencedColumnName = "id", insertable = false, updatable = false)
    private ProductAttributeEntity attribute;

    /**
     * Nombre descriptivo del valor (máximo 100 caracteres)
     * Ej: "Rojo", "Azul", "S", "M", "L"
     */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * Color HTML asociado al valor (opcional)
     * Formato: #RRGGBB (6 dígitos hexadecimales)
     * Máximo 7 caracteres (#RRGGBB)
     */
    @Column(name = "html_color", length = 7)
    private String htmlColor;

    /**
     * Secuencia para ordenamiento
     * Por defecto: 0
     */
    @Column(name = "sequence", columnDefinition = "INTEGER DEFAULT 0")
    private Integer sequence;

    /**
     * Indica si el valor está activo
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
