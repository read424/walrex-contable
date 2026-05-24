package org.walrex.domain.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.walrex.application.port.input.FinnhubTradeUseCase;
import org.walrex.application.port.input.UpdateAstroPayExchangeRateUseCase;
import org.walrex.application.port.input.UpdateExchangeRatesUseCase;
import org.walrex.application.port.output.MarketPriceTickPort;
import org.walrex.domain.model.MarketPriceTick;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@ApplicationScoped
public class FinnhubTradeService implements FinnhubTradeUseCase {

    private static final String PROVIDER = "FINNHUB";

    @ConfigProperty(name = "finnhub.price-change-threshold", defaultValue = "2.0")
    BigDecimal priceChangeThreshold;

    @Inject
    MarketPriceTickPort tickPort;

    @Inject
    UpdateAstroPayExchangeRateUseCase updateAstroPayUseCase;

    @Inject
    UpdateExchangeRatesUseCase updateExchangeRatesUseCase;

    @Inject
    DerivedExchangeRateService derivedRateService;

    private final ConcurrentHashMap<String, BigDecimal> lastPrices = new ConcurrentHashMap<>();

    @Override
    public Uni<Void> onTrade(String symbol, BigDecimal price, Instant tradeTime) {
        BigDecimal previous = lastPrices.put(symbol, price);
        String[] currencies = parseCurrencyPair(symbol);

        boolean isFirstSnapshot = (previous == null);
        BigDecimal changePct = null;

        if (!isFirstSnapshot && previous.compareTo(BigDecimal.ZERO) != 0) {
            changePct = price.subtract(previous)
                    .divide(previous, 6, RoundingMode.HALF_UP)
                    .abs()
                    .multiply(BigDecimal.valueOf(100));
        }

        final BigDecimal finalChangePct = changePct;

        MarketPriceTick tick = MarketPriceTick.builder()
                .provider(PROVIDER)
                .symbol(symbol)
                .currencyBase(currencies != null ? currencies[0] : "")
                .currencyQuote(currencies != null ? currencies[1] : "")
                .price(price)
                .eventType(isFirstSnapshot ? "SUBSCRIBE" : "TICK")
                .changePct(changePct)
                .recordedAt(tradeTime)
                .build();

        // Siempre registrar el tick; si falla no bloquea el flujo principal
        return tickPort.record(tick)
                .onFailure().invoke(e -> log.error("[FinnhubTrade] Error guardando tick {}: {}", symbol, e.getMessage()))
                .onFailure().recoverWithNull()
                .chain(() -> {
                    if (isFirstSnapshot) {
                        log.info("[FinnhubTrade] {} primer snapshot price={}", symbol, price);
                        return Uni.createFrom().voidItem();
                    }

                    log.info("[FinnhubTrade] {} price={} previous={} change={}%",
                            symbol, price, previous,
                            finalChangePct.setScale(4, RoundingMode.HALF_UP));

                    if (finalChangePct.compareTo(priceChangeThreshold) < 0) {
                        return Uni.createFrom().voidItem();
                    }

                    // El par USD/{currency} superó el umbral — refrescar tasas
                    String affectedCurrency = extractAffectedCurrency(currencies);
                    log.info("[FinnhubTrade] {} superó {}% — actualizando rutas para {}",
                             symbol, priceChangeThreshold, affectedCurrency);

                    return updateAstroPayUseCase.updateRatesForCurrency(affectedCurrency)
                            .chain(() -> updateExchangeRatesUseCase.updateExchangeRates().replaceWithVoid())
                            .chain(() -> derivedRateService.updateEurVes())
                            .invoke(() -> log.info("[FinnhubTrade] Actualización completa (trigger: {})", symbol))
                            .onFailure().invoke(e ->
                                    log.error("[FinnhubTrade] Error en actualización tras {}: {}", symbol, e.getMessage()))
                            .onFailure().recoverWithNull();
                });
    }

    // "OANDA:EUR_USD" → ["EUR","USD"] | "OANDA:USD_COP" → ["USD","COP"]
    // "BINANCE:USDTBRL" → ["USDT","BRL"]
    private String[] parseCurrencyPair(String symbol) {
        int colon = symbol.indexOf(':');
        if (colon < 0) return null;
        String pair = symbol.substring(colon + 1);
        if (pair.contains("_")) {
            String[] parts = pair.split("_");
            return parts.length == 2 ? parts : null;
        }
        if (pair.toUpperCase().startsWith("USDT") && pair.length() > 4) {
            return new String[]{"USDT", pair.substring(4).toUpperCase()};
        }
        return null;
    }

    // ["USD","COP"] → "COP" | ["EUR","USD"] → "EUR" | ["USDT","BRL"] → "BRL"
    private String extractAffectedCurrency(String[] currencies) {
        if (currencies == null) return "";
        String base = currencies[0];
        return "USD".equalsIgnoreCase(base) || "USDT".equalsIgnoreCase(base)
                ? currencies[1]
                : base;
    }
}
