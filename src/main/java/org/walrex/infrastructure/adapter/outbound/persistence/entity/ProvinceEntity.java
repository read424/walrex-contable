package org.walrex.infrastructure.adapter.outbound.persistence.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
@ToString(exclude = "departament")
@EqualsAndHashCode(callSuper = true, exclude = "departament")
@Entity
@Table(name = "province", uniqueConstraints = {
        @UniqueConstraint(name = "province_code_uk", columnNames = {"cod_province"}),
        @UniqueConstraint(name = "province_name_uk", columnNames = {"name_province"})
})
public class ProvinceEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_province")
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_departament", nullable = false, referencedColumnName = "id_departament")
    private DepartamentEntity departament;

    @Column(name = "cod_province", nullable = false, unique = true)
    private String codigo;

    @Column(name = "name_province", nullable = false, unique = true)
    private String name;

    @Column(name = "status", columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean status = true;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
