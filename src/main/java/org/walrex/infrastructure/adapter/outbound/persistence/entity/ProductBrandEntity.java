package org.walrex.infrastructure.adapter.outbound.persistence.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
@ToString
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "product_brand", uniqueConstraints = {
        @UniqueConstraint(name = "product_brand_pkey", columnNames = {"id"}),
        @UniqueConstraint(name = "product_brand_name_uk", columnNames = {"name"})
})
public class ProductBrandEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String name;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;
}
