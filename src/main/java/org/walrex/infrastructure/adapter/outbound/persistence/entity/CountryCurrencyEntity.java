package org.walrex.infrastructure.adapter.outbound.persistence.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@ToString
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "country_currencies", uniqueConstraints = {
        @UniqueConstraint(name = "uk_country_currency", columnNames = {"country_id", "currency_id"})
})
public class CountryCurrencyEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "country_id", nullable = false)
    private CountryEntity country;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "currency_id", nullable = false)
    private CurrencyEntity currency;

    @Column(name = "is_primary")
    private Boolean isPrimary;

    @Column(name = "is_operational")
    private Boolean isOperational;

    @Column(name = "effective_date")
    private LocalDate effectiveDate;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;
}