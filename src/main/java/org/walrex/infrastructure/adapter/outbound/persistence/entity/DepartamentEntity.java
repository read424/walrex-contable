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
@Table(name = "departament", uniqueConstraints = {
        @UniqueConstraint(name = "departament_name_uk", columnNames = {"nombre"}),
        @UniqueConstraint(name = "departament_code_uk", columnNames = {"codigo"})
})
public class DepartamentEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_departament")
    private Integer id;

    @Column(name = "cod_departament", nullable = false, unique = true)
    private String codigo;

    @Column(name = "name_departament", nullable = false, unique = true)
    private String nombre;

    @Column(name = "status", columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean status = true;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime udpdatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;
}
