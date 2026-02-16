package org.walrex.infrastructure.adapter.outbound.persistence.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(name = "beneficiary")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BeneficiaryEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "client_id", nullable = false)
    public Integer clientId;

    @Column(name = "country_id", nullable = false)
    public Integer countryId;

    @Column(name = "first_name", nullable = false, length = 80)
    public String firstName;

    @Column(name = "last_name", nullable = false, length = 80)
    public String lastName;

    @Column(name = "document_type", nullable = false)
    public Integer documentType;

    @Column(name = "document_number", length = 20)
    public String documentNumber;

    @Column(length = 80)
    public String alias;

    @Column(length = 1)
    @Builder.Default
    public String status = "1";

    @Column(name = "created_at", updatable = false)
    public OffsetDateTime createdAt;

    @Column(name = "updated_at")
    public OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
