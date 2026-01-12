package org.walrex.infrastructure.adapter.outbound.persistence.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.*;
import org.walrex.domain.model.AttributeDisplayType;
import org.walrex.infrastructure.adapter.outbound.persistence.converter.AttributeDisplayTypeConverter;

import java.time.OffsetDateTime;

/**
 * Entidad JPA para atributos de producto.
 *
 * Mapea la tabla 'product_attributes' en PostgreSQL.
 * Usa Panache para simplificar operaciones de persistencia reactiva.
 *
 * IMPORTANTE: El ID es de tipo Integer auto-generado.
 *
 * Configuración:
 * - Extends PanacheEntityBase: Proporciona métodos helper de Panache
 * - @Entity: Marca esta clase como entidad JPA
 * - @Table: Especifica el nombre de la tabla
 * - Lombok: Genera getters, setters, constructores, etc.
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@ToString
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "product_attributes", uniqueConstraints = {
    @UniqueConstraint(name = "uk_product_attribute_name", columnNames = { "name" })
})
public class ProductAttributeEntity extends PanacheEntityBase {

    /**
     * Identificador único del atributo (auto-generado)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    /**
     * Nombre descriptivo del atributo (máximo 100 caracteres)
     */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * Tipo de visualización del atributo
     * Almacenado como VARCHAR(20) en DB (lowercase: select, radio, color, text)
     * Convertido automáticamente a/desde enum AttributeDisplayType
     */
    @Column(name = "display_type", nullable = false, length = 20, columnDefinition = "VARCHAR(20) DEFAULT 'select'")
    @Convert(converter = AttributeDisplayTypeConverter.class)
    private AttributeDisplayType displayType;

    /**
     * Indica si el atributo está activo
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
