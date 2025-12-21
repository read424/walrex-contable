package org.walrex.infrastructure.adapter.outbound.persistence.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
@ToString(exclude = "province")
@EqualsAndHashCode(callSuper = true, exclude = "province")
@Entity
@Table(name = "district", uniqueConstraints = {
        @UniqueConstraint(name = "district_code_uk", columnNames = {"cod_district"}),
        @UniqueConstraint(name = "district_name_uk", columnNames = {"name_district", "id_province"})
})
public class DistrictEntity extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_district")
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_province", nullable = false, referencedColumnName = "id_province")
    private ProvinceEntity province;

    @Column(name = "cod_district", nullable = false, unique = true, columnDefinition = "CHAR(6)")
    private String codigo;

    @Column(name = "name_district", nullable = false, unique = true)
    private String name;

    @Column(name = "status", columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean status = true;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
