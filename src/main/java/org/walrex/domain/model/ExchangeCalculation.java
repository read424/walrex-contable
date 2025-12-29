package org.walrex.domain.model;

import java.math.BigDecimal;

/**
 * Modelo de dominio que representa el resultado de un cálculo de cambio de divisas.
 *
 * Contiene todos los detalles del cálculo realizado para transparencia y auditoría.
 */
public record ExchangeCalculation(
        /**
         * Monto original en la moneda base.
         */
        BigDecimal amount,

        /**
         * Código de la moneda base (origen).
         */
        String baseCurrency,

        /**
         * Código de la moneda cotizada (destino).
         */
        String quoteCurrency,

        /**
         * Precio promedio de compra de USDT con la moneda base.
         */
        BigDecimal averageBuyPrice,

        /**
         * Precio promedio de venta de USDT por la moneda cotizada.
         */
        BigDecimal averageSellPrice,

        /**
         * Tasa de cambio cruzada sin margen.
         * Calculado como: averageSellPrice / averageBuyPrice
         */
        BigDecimal rate,

        /**
         * Tasa de cambio con margen aplicado.
         */
        BigDecimal exchangeRate,

        /**
         * Monto convertido en la moneda cotizada.
         */
        BigDecimal convertedAmount,

        /**
         * Margen aplicado al cálculo (en porcentaje).
         */
        BigDecimal marginApplied
) {
}
