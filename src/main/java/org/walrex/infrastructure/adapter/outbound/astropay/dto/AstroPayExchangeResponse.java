package org.walrex.infrastructure.adapter.outbound.astropay.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Respuesta de POST /v1/public/exchanges
 *
 * Ejemplo:
 * {
 *   "from": "PEN",
 *   "to": "EUR",
 *   "official_exchange": 0.245932446007,
 *   "exchange": 0.243473121547,
 *   "spread": 1.00,
 *   "spread_amount": -0.002459324460,
 *   "exchange_without_spread": 0.245932446007
 * }
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AstroPayExchangeResponse {

    private String from;

    private String to;

    @JsonProperty("official_exchange")
    private BigDecimal officialExchange;

    private BigDecimal exchange;

    private BigDecimal spread;

    @JsonProperty("spread_amount")
    private BigDecimal spreadAmount;

    @JsonProperty("exchange_without_spread")
    private BigDecimal exchangeWithoutSpread;
}
