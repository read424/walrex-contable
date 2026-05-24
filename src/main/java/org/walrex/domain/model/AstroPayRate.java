package org.walrex.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;

@Builder
@Data
@AllArgsConstructor
@ToString
public class AstroPayRate {

    /** Moneda origen (ISO 4217, ej: "PEN") */
    private String from;

    /** Moneda destino (ISO 4217, ej: "EUR") */
    private String to;

    /** Tasa oficial sin spread */
    private BigDecimal officialExchange;

    /** Tasa con spread aplicado (la que ve el usuario) */
    private BigDecimal exchange;

    /** Porcentaje de spread */
    private BigDecimal spread;

    /** Monto del spread sobre la tasa oficial */
    private BigDecimal spreadAmount;

    /** Tasa bruta sin spread (equivalente a officialExchange) */
    private BigDecimal exchangeWithoutSpread;
}
