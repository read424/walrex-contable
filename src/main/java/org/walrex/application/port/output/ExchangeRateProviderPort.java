package org.walrex.application.port.output;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.ExchangeRate;

import java.math.BigDecimal;
import java.util.List;

/**
 * Puerto de salida para consultar tasas de cambio de proveedores externos
 * (Binance, otros exchanges, etc.)
 */
public interface ExchangeRateProviderPort {

    /**
     * Consulta las tasas de cambio P2P disponibles
     *
     * @param asset Moneda base (ej: USDT)
     * @param fiat Moneda fiat (ej: PEN, VES)
     * @param tradeType Tipo de operación (BUY o SELL)
     * @param payTypes Métodos de pago opcionales
     * @param transAmount Monto de transacción opcional (en fiat)
     * @return Lista de tasas disponibles
     */
    Uni<List<ExchangeRate>> fetchExchangeRates(
            String asset,
            String fiat,
            String tradeType,
            List<String> payTypes,
            BigDecimal transAmount
    );

    /**
     * Consulta las tasas de cambio P2P disponibles con monto en crypto
     *
     * @param asset Moneda base (ej: USDT)
     * @param fiat Moneda fiat (ej: PEN, VES)
     * @param tradeType Tipo de operación (BUY o SELL)
     * @param payTypes Métodos de pago opcionales
     * @param transAmount Monto de transacción en fiat (para BUY)
     * @param transCryptoAmount Monto de transacción en crypto (para SELL)
     * @return Lista de tasas disponibles
     */
    Uni<List<ExchangeRate>> fetchExchangeRates(
            String asset,
            String fiat,
            String tradeType,
            List<String> payTypes,
            BigDecimal transAmount,
            BigDecimal transCryptoAmount
    );
}
