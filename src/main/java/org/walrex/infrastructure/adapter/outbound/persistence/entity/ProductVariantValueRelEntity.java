package org.walrex.infrastructure.adapter.outbound.persistence.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

/**
 * Entidad JPA para la relación entre variantes y valores de atributos.
 *
 * Mapea la tabla 'product_variant_value_rel' en PostgreSQL.
 * Define la combinación de valores de atributos de cada variante.
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@ToString
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "product_variant_value_rel")
@IdClass(ProductVariantValueRelEntity.CompositeKey.class)
public class ProductVariantValueRelEntity extends PanacheEntityBase {

    @Id
    @Column(name = "variant_id", nullable = false)
    private Integer variantId;

    @Id
    @Column(name = "value_id", nullable = false)
    private Integer valueId;

    /**
     * Relación con el valor del atributo.
     * IMPORTANTE: Usar JOIN FETCH en queries para evitar N+1 queries.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "value_id", referencedColumnName = "id", insertable = false, updatable = false)
    private ProductAttributeValueEntity attributeValue;

    /**
     * Clave compuesta para la tabla de relación.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompositeKey implements Serializable {
        private Integer variantId;
        private Integer valueId;
    }
}
