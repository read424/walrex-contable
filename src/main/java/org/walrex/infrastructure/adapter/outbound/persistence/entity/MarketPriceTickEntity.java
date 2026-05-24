package org.walrex.infrastructure.adapter.outbound.persistence.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "market_price_tick")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class MarketPriceTickEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider", nullable = false, length = 20)
    private String provider;

    @Column(name = "symbol", nullable = false, length = 50)
    private String symbol;

    @Column(name = "currency_base", nullable = false, length = 10)
    private String currencyBase;

    @Column(name = "currency_quote", nullable = false, length = 10)
    private String currencyQuote;

    @Column(name = "price", nullable = false, precision = 20, scale = 8)
    private BigDecimal price;

    @Column(name = "event_type", nullable = false, length = 20)
    private String eventType;

    @Column(name = "change_pct", precision = 10, scale = 6)
    private BigDecimal changePct;

    @Column(name = "recorded_at", nullable = false)
    private OffsetDateTime recordedAt;
}
