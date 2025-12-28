package org.walrex.infrastructure.adapter.outbound.persistence.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entidad para rutas de remesas
 * Define pares país-moneda origen/destino para consultar tasas de cambio
 */
@Entity
@Table(name = "remittance_routes")
@Getter
@Setter
public class RemittanceRouteEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_remittance_country", nullable = false)
    private RemittanceCountryEntity remittanceCountry;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_country_currencies_from", nullable = false)
    private CountryCurrencyEntity countryCurrencyFrom;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_country_currencies_to", nullable = false)
    private CountryCurrencyEntity countryCurrencyTo;

    @Column(name = "intermediary_asset", length = 10, nullable = false)
    private String intermediaryAsset = "USDT";

    @Column(name = "is_active", length = 1, nullable = false)
    private String isActive = "1";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
