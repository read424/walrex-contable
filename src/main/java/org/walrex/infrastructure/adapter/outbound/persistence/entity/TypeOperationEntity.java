package org.walrex.infrastructure.adapter.outbound.persistence.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "type_operation")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TypeOperationEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Integer id;

    @Column(name = "det_name", nullable = false, length = 50)
    public String name;

    @Column(name = "requires_bank")
    public Boolean requiresBank;

    @Column(name = "label_helper", length = 50)
    public String labelHelper;

    @Column(length = 1)
    public String status;
}
