package org.walrex.domain.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.walrex.application.port.input.CalculateExchangeRateUseCase;
import org.walrex.application.port.output.ExchangeRateProviderPort;
import org.walrex.application.port.output.PaymentMethodQueryPort;
import org.walrex.application.port.output.RemittanceRouteOutputPort;
import org.walrex.domain.exception.ExchangeRateTimeoutException;
import org.walrex.domain.model.ExchangeCalculation;
import org.walrex.domain.model.ExchangeRate;
import org.walrex.domain.model.ExchangeRateRouteInfo;
import org.walrex.domain.model.RemittanceRoute;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * Servicio de dominio que implementa la lógica de cálculo de cambio de divisas.
 *
 * Responsabilidades:
 * - Validar que existe una ruta configurada para el par de monedas
 * - Consultar tasas en tiempo real de Binance P2P
 * - Calcular conversión cruzada usando USDT como intermediario
 * - Aplicar margen de ganancia
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class ExchangeRateCalculationService implements CalculateExchangeRateUseCase {

    private static final String USDT = "USDT";
    private static final String TRADE_TYPE_BUY = "BUY";
    private static final String TRADE_TYPE_SELL = "SELL";
    private static final int TOP_PRICES_LIMIT = 5;
    private static final int SCALE = 8;

    // Timeout de 25 segundos para el cálculo completo
    // (menor que el timeout de 30s del RestClient para detectar antes)
    private static final Duration CALCULATION_TIMEOUT = Duration.ofSeconds(25);

    private final RemittanceRouteOutputPort remittanceRoutePort;
    private final PaymentMethodQueryPort paymentMethodPort;
    private final ExchangeRateProviderPort exchangeRateProvider;

    @Override
    public Uni<ExchangeCalculation> calculateExchangeRate(
            BigDecimal amount,
            String baseCountry,
            String baseCurrency,
            String quoteCountry,
            String quoteCurrency,
            BigDecimal margin) {

        log.info("Calculating exchange rate: {} {} -> {}, margin: {}%",
                amount, baseCurrency, quoteCurrency, margin);

        return remittanceRoutePort.findAllActiveExchangeRateRoutes()
                .map(routes -> routes.stream()
                        .filter(route -> matchesCurrencyPair(route, baseCountry, baseCurrency, quoteCountry, quoteCurrency))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException(
                                String.format("No active remittance route found for %s (%s) -> %s (%s)",
                                        baseCountry, baseCurrency, quoteCountry, quoteCurrency)))
                )
                .flatMap(route -> calculateCrossRate(amount, route, margin))
                // Aplicar timeout proactivo de 25 segundos
                .ifNoItem().after(CALCULATION_TIMEOUT).failWith(() -> {
                    log.error("Exchange rate calculation timed out after {} seconds for {} -> {}",
                            CALCULATION_TIMEOUT.getSeconds(), baseCurrency, quoteCurrency);
                    return new ExchangeRateTimeoutException(
                            String.format("El cálculo de tipo de cambio excedió el tiempo máximo de espera (%d segundos). " +
                                    "Por favor, intente nuevamente.", CALCULATION_TIMEOUT.getSeconds()));
                })
                // Convertir TimeoutException de operaciones reactivas
                .onFailure(TimeoutException.class).transform(error -> {
                    log.error("Reactive timeout during exchange rate calculation for {} -> {}",
                            baseCurrency, quoteCurrency, error);
                    return new ExchangeRateTimeoutException(
                            String.format("El cálculo de tipo de cambio excedió el tiempo máximo de espera. " +
                                    "Por favor, intente nuevamente."), error);
                });
    }

    /**
     * Verifica si la ruta coincide con el par solicitado.
     */
    private boolean matchesCurrencyPair(ExchangeRateRouteInfo route, String baseCountry, String baseCurrency, String quoteCountry, String quoteCurrency) {
        return baseCountry.equalsIgnoreCase(route.getCountryFromCode()) && baseCurrency.equalsIgnoreCase(route.getCurrencyFromCode())
                && quoteCountry.equalsIgnoreCase(route.getCountryToCode()) && quoteCurrency.equalsIgnoreCase(route.getCurrencyToCode());
    }

    private Uni<ExchangeCalculation> calculateCrossRate(
            BigDecimal amount,
            ExchangeRateRouteInfo route,
            BigDecimal margin) {

        log.info("Calculating cross rate for route: {}:{} -> {}:{}", 
                route.getCountryFromCode(), route.getCurrencyFromCode(), 
                route.getCountryToCode(), route.getCurrencyToCode());

        // Consultar métodos de pago y tasas en paralelo
        Uni<BigDecimal> buyPriceUni = fetchAveragePriceForPair(route, true);
        Uni<BigDecimal> sellPriceUni = fetchAveragePriceForPair(route, false);

        return Uni.combine().all().unis(buyPriceUni, sellPriceUni).asTuple()
                .map(tuple -> {
                    BigDecimal avgBuyPrice = tuple.getItem1();
                    BigDecimal avgSellPrice = tuple.getItem2();

                    // Calcular USDT recibido
                    BigDecimal usdtReceived = calculateUsdtReceived(amount, avgBuyPrice);

                    return buildExchangeCalculation(
                            amount,
                            route.getCurrencyFromCode(),
                            route.getCurrencyToCode(),
                            avgBuyPrice,
                            avgSellPrice,
                            usdtReceived,
                            margin
                    );
                });
    }

    /**
     * Obtiene el precio promedio (Top 5) para compra o venta.
     */
    private Uni<BigDecimal> fetchAveragePriceForPair(ExchangeRateRouteInfo route, boolean isBuy) {
        Long countryCurrencyId = isBuy ? route.getCountryCurrencyFromId() : route.getCountryCurrencyToId();
        String currency = isBuy ? route.getCurrencyFromCode() : route.getCurrencyToCode();
        String tradeType = isBuy ? TRADE_TYPE_BUY : TRADE_TYPE_SELL;

        return paymentMethodPort.findBinancePaymentCodesByCountryCurrency(countryCurrencyId)
                .flatMap(payTypes -> exchangeRateProvider.fetchExchangeRates(
                        USDT,
                        currency,
                        tradeType,
                        payTypes,
                        null
                ))
                .map(rates -> calculateTop5Average(rates, isBuy));
    }

    /**
     * Calcula el promedio de las 5 mejores tasas.
     */
    private BigDecimal calculateTop5Average(List<ExchangeRate> rates, boolean isBuy) {
        if (rates == null || rates.isEmpty()) {
            throw new IllegalStateException("No exchange rates available for " + (isBuy ? "BUY" : "SELL"));
        }

        List<BigDecimal> topPrices = rates.stream()
                .sorted(isBuy 
                        ? Comparator.comparing(ExchangeRate::price)           // Compra: más barato primero
                        : Comparator.comparing(ExchangeRate::price).reversed()) // Venta: más caro primero
                .limit(TOP_PRICES_LIMIT)
                .map(ExchangeRate::price)
                .toList();

        BigDecimal sum = topPrices.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(topPrices.size()), SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Calcula la cantidad de USDT recibido al comprar con la moneda base.
     */
    private BigDecimal calculateUsdtReceived(BigDecimal amount, BigDecimal buyPrice) {
        return amount.divide(buyPrice, SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Construye el resultado del cálculo con todos los detalles.
     */
    private ExchangeCalculation buildExchangeCalculation(
            BigDecimal amount,
            String baseCurrency,
            String quoteCurrency,
            BigDecimal avgBuyPrice,
            BigDecimal avgSellPrice,
            BigDecimal usdtReceived,
            BigDecimal margin) {

        // Tasa cruzada sin margen
        BigDecimal rate = avgSellPrice.divide(avgBuyPrice, SCALE, RoundingMode.HALF_UP);

        // Tasa con margen aplicado (dividiendo para reducir el monto que damos)
        BigDecimal marginMultiplier = BigDecimal.ONE.add(margin.divide(BigDecimal.valueOf(100), SCALE, RoundingMode.HALF_UP));
        BigDecimal exchangeRate = rate.divide(marginMultiplier, SCALE, RoundingMode.HALF_UP);

        // Monto convertido
        BigDecimal convertedAmount = amount.multiply(exchangeRate).setScale(2, RoundingMode.HALF_UP);

        log.info("Cross rate calculated: {} {} -> {} {} | Rate: {} | With margin: {} | Converted: {}",
                amount, baseCurrency, convertedAmount, quoteCurrency, rate, exchangeRate, convertedAmount);

        return new ExchangeCalculation(
                amount,
                baseCurrency,
                quoteCurrency,
                avgBuyPrice,
                avgSellPrice,
                rate,
                exchangeRate,
                convertedAmount,
                margin
        );
    }
}
