package org.walrex.infrastructure.adapter.outbound.persistence.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * Entidad JPA para categorías de unidades de medida.
 *
 * Mapea la tabla 'product_category_uom' en PostgreSQL.
 * Usa Panache para simplificar operaciones de persistencia reactiva.
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
@ToString
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "product_category_uom", uniqueConstraints = {
        @UniqueConstraint(name = "uk_category_uom_code", columnNames = { "code" })
})
public class ProductCategoryUomEntity extends PanacheEntityBase {

    /**
     * Identificador único generado automáticamente
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * Código único de la categoría (máximo 20 caracteres)
     */
    @Column(name = "code", nullable = false, unique = true, length = 20)
    private String code;

    /**
     * Nombre descriptivo de la categoría (máximo 100 caracteres)
     */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * Descripción detallada de la categoría (máximo 255 caracteres)
     */
    @Column(name = "description", length = 255)
    private String description;

    /**
     * Indica si la categoría está activa
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
