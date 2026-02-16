package org.walrex.infrastructure.adapter.outbound.persistence.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@ToString
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "product_category", uniqueConstraints = {
        @UniqueConstraint(name = "product_category_pkey", columnNames = {"id"}),
        @UniqueConstraint(name = "product_category_name_uk", columnNames = { "name", "parentId" })
})
public class CategoryProductEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @Column(name = "parent_id", insertable = false, updatable = false)
    private Integer parentId;

    // Relación hacia el PADRE (Many categories -> One parent)
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "parent_id", foreignKey = @ForeignKey(name = "product_category_parent_id_fkey"))
    private CategoryProductEntity parent;

    // Relación hacia los HIJOS (One parent -> Many children)
    @OneToMany(mappedBy = "parent", fetch = FetchType.EAGER)
    private List<CategoryProductEntity> children = new ArrayList<>();

    @Column(name = "created_at")
    private OffsetDateTime createdAt;
}
