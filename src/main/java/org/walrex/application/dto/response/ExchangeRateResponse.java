package org.walrex.application.dto.response;

import org.walrex.domain.model.ExchangeRate;
import java.math.BigDecimal;
import java.util.List;

/**
 * DTO de respuesta para cálculo de cambio de divisas.
 *
 * Incluye detalles del cálculo realizado para transparencia.
 */
public record ExchangeRateResponse(
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
         * Precio promedio de compra de USDT.
         */
        BigDecimal averageBuyPrice,

        /**
         * Precio promedio de venta de USDT.
         */
        BigDecimal averageSellPrice,

        /**
         * Tasa de cambio sin margen aplicado.
         * Calculado como: averageSellPrice / averageBuyPrice
         */
        BigDecimal rate,

        /**
         * Tasa de cambio con margen aplicado.
         * Aplicado dividiendo: rate / (1 + margin/100)
         */
        BigDecimal exchangeRate,

        /**
         * Monto convertido en la moneda cotizada.
         * Calculado como: amount * exchangeRate
         */
        BigDecimal convertedAmount,

        /**
         * Margen aplicado al cálculo (en porcentaje).
         */
        BigDecimal marginApplied,

        /**
         * Listado de tasas de compra encontradas (Top 5).
         */
        List<ExchangeRate> advPriceBuy,

        /**
         * Listado de tasas de venta encontradas (Top 5).
         */
        List<ExchangeRate> advPriceSell,

        /**
         * Monto de USDT recibido en el primer tramo del cálculo.
         */
        BigDecimal usdtReceived
) {
}
