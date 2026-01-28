package org.walrex.infrastructure.adapter.outbound.persistence.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Data
@ToString
@EqualsAndHashCode(callSuper = false)
@Builder
@Entity
@Table(name = "account_wallet", uniqueConstraints = {
        @UniqueConstraint(name = "uq_wallet_client_currency", columnNames = {"client_id", "country_id", "currency_id"})
})
public class AccountWalletEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_id", nullable = false)
    private Integer clientId;

    @Column(name = "country_id", nullable = false)
    private Integer countryId;

    @Column(name = "currency_id", nullable = false)
    private Integer currencyId;

    @Builder.Default
    @Column(name = "available_balance", nullable = false, precision = 18, scale = 2)
    private BigDecimal availableBalance = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "ledger_balance", nullable = false, precision = 18, scale = 2)
    private BigDecimal ledgerBalance = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "is_balance_visible", nullable = false)
    private Boolean isBalanceVisible = true;

    @Column(name = "bank_account_number", length = 34)
    private String bankAccountNumber;

    @Builder.Default
    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
