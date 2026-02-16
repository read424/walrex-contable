package org.walrex.infrastructure.adapter.outbound.persistence.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(name = "beneficiary_account")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BeneficiaryAccountEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "beneficiary_id", nullable = false)
    public BeneficiaryEntity beneficiary;

    @Column(name = "payout_rail_id", nullable = false)
    public Integer payoutRailId;

    @Column(name = "bank_id")
    public Long bankId;

    @Column(name = "account_number", length = 40)
    public String accountNumber;

    @Column(name = "phone_number", length = 20)
    public String phoneNumber;

    @Column(name = "currency_id", nullable = false)
    public Integer currencyId;

    @Column(name = "is_favorite")
    @Builder.Default
    public Boolean isFavorite = false;

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
