package org.walrex.infrastructure.adapter.outbound.persistence.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * Entidad para la tabla country_currency_payment_methods
 * Junction table que asocia country_currencies con banks (payment methods)
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@ToString
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "country_currency_payment_methods", uniqueConstraints = {
    @UniqueConstraint(name = "uk_ccpm_country_currency_bank",
                     columnNames = {"id_country_currency", "id_bank"})
})
public class CountryCurrencyPaymentMethodEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_country_currency", nullable = false)
    private CountryCurrencyEntity countryCurrency;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_bank", nullable = false)
    private BankEntity bank;

    @Column(name = "is_active", length = 1, nullable = false)
    private String isActive = "1";

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
