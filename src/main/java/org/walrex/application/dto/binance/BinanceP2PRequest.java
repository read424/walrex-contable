package org.walrex.application.dto.binance;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO para request a Binance P2P API
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BinanceP2PRequest(
        Integer additionalKycVerifyFilter,
        String asset,              // currency_base (ej: USDT)
        List<String> classifies,
        List<String> countries,
        String fiat,               // currency_quote (ej: PEN, VES)
        String filterType,
        Integer page,
        List<String> payTypes,     // Opcional
        List<String> periods,
        Boolean proMerchantAds,
        String publisherType,
        Integer rows,
        Boolean shieldMerchantAds,
        String tradeType,          // BUY o SELL
        BigDecimal transAmount,    // Opcional - monto en fiat para BUY
        BigDecimal transCryptoAmount // Opcional - monto en crypto para SELL
) {
    /**
     * Crea un request por defecto para consultar tasas
     */
    public static BinanceP2PRequest createDefault(String asset, String fiat, String tradeType) {
        return BinanceP2PRequest.builder()
                .additionalKycVerifyFilter(0)
                .asset(asset)
                .classifies(List.of("mass", "profession", "fiat_trade"))
                .countries(List.of())
                .fiat(fiat)
                .filterType("all")
                .page(1)
                .payTypes(List.of())
                .periods(List.of())
                .proMerchantAds(false)
                .publisherType(null)
                .rows(10)
                .shieldMerchantAds(false)
                .tradeType(tradeType)
                .transAmount(null)
                .transCryptoAmount(null)
                .build();
    }
}
