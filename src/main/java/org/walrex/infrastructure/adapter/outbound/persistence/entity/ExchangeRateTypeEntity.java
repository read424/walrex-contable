package org.walrex.infrastructure.adapter.outbound.persistence.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "exchange_rate_types")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeRateTypeEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "country_id", nullable = false)
    private Integer countryId;

    @Column(name = "date_rate", nullable = false)
    private LocalDate dateRate;

    @Column(name = "code_rate", nullable = false, length = 20)
    private String codeRate;

    @Column(name = "name_rate", nullable = false, length = 100)
    private String nameRate;

    @Column(name = "rate_value", nullable = false, precision = 15, scale = 6)
    private BigDecimal rateValue;

    @Column(name = "base_currency_id")
    private Integer baseCurrencyId;

    @Column(name = "is_active", nullable = false, columnDefinition = "char(1)")
    private String isActive;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    // Relaciones
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "country_id", insertable = false, updatable = false)
    private CountryEntity country;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "base_currency_id", insertable = false, updatable = false)
    private CurrencyEntity baseCurrency;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = OffsetDateTime.now();
        }
        if (isActive == null) {
            isActive = "1";
        }
        if (displayOrder == null) {
            displayOrder = 0;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}