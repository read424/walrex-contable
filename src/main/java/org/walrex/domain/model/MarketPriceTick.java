package org.walrex.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketPriceTick {
    private String provider;       // "FINNHUB"
    private String symbol;         // "OANDA:USD_COP"
    private String currencyBase;   // "USD"
    private String currencyQuote;  // "COP"
    private BigDecimal price;
    private String eventType;      // "SUBSCRIBE" or "TICK"
    private BigDecimal changePct;  // null for SUBSCRIBE
    private Instant recordedAt;
}
