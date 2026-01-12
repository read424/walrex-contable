package org.walrex.infrastructure.adapter.outbound.persistence.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

/**
 * Entidad JPA para líneas de atributos de plantillas de producto.
 *
 * Mapea la tabla 'product_template_attribute_line' en PostgreSQL.
 * Define qué atributos están disponibles para un producto.
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@ToString
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "product_template_attribute_line", uniqueConstraints = {
        @UniqueConstraint(name = "uk_template_attribute",
                columnNames = {"product_template_id", "attribute_id"})
})
public class ProductTemplateAttributeLineEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "product_template_id", nullable = false)
    private Integer productTemplateId;

    @Column(name = "attribute_id", nullable = false)
    private Integer attributeId;

    /**
     * Relación con el atributo.
     * IMPORTANTE: Usar JOIN FETCH en queries para evitar N+1 queries.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attribute_id", referencedColumnName = "id", insertable = false, updatable = false)
    private ProductAttributeEntity attribute;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
