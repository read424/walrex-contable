package org.walrex.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Modelo de dominio para tasas de cambio entre monedas
 */
public record ExchangeRate(
        String advNo,
        String currencyBase,     // Ej: USDT
        String currencyQuote,    // Ej: PEN, VES
        String tradeType,        // BUY o SELL
        BigDecimal price,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        Integer payTimeLimit,
        List<PaymentMethod> paymentMethods,
        String merchantNickName,
        String classify,
        LocalDateTime fetchedAt
) {
    public record PaymentMethod(
            String shortName,
            String fullName
    ) {}
}
