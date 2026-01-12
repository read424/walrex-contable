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
import org.walrex.domain.model.RemittanceRoute;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
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
            String baseCurrency,
            String quoteCurrency,
            BigDecimal margin) {

        log.info("Calculating exchange rate: {} {} -> {}, margin: {}%",
                amount, baseCurrency, quoteCurrency, margin);

        return findRemittanceRoute(baseCurrency, quoteCurrency)
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
     * Busca la ruta de remesa configurada para el par de monedas.
     */
    private Uni<RemittanceRoute> findRemittanceRoute(String baseCurrency, String quoteCurrency) {
        return remittanceRoutePort.findAllActiveRoutes()
                .map(routes -> routes.stream()
                        .filter(route -> matchesCurrencyPair(route, baseCurrency, quoteCurrency))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException(
                                String.format("No active remittance route found for %s -> %s",
                                        baseCurrency, quoteCurrency)))
                );
    }

    /**
     * Verifica si la ruta coincide con el par de monedas solicitado.
     */
    private boolean matchesCurrencyPair(RemittanceRoute route, String baseCurrency, String quoteCurrency) {
        return baseCurrency.equals(route.getCurrencyFromCode())
                && quoteCurrency.equals(route.getCurrencyToCode());
    }

    /**
     * Calcula la tasa cruzada usando USDT como intermediario.
     */
    private Uni<ExchangeCalculation> calculateCrossRate(
            BigDecimal amount,
            RemittanceRoute route,
            BigDecimal margin) {

        // Paso 1: Comprar USDT con la moneda base
        Uni<BigDecimal> buyPriceUni = fetchAverageBuyPrice(route);
        Uni<List<String>> buyPaymentMethodsUni = paymentMethodPort
                .findBinancePaymentCodesByCountryCurrency(route.getCountryCurrencyFromId());

        // Paso 2: Vender USDT por la moneda cotizada
        Uni<List<String>> sellPaymentMethodsUni = paymentMethodPort
                .findBinancePaymentCodesByCountryCurrency(route.getCountryCurrencyToId());

        return Uni.combine().all()
                .unis(buyPriceUni, buyPaymentMethodsUni, sellPaymentMethodsUni)
                .asTuple()
                .flatMap(tuple -> {
                    BigDecimal avgBuyPrice = tuple.getItem1();
                    List<String> buyPayMethods = tuple.getItem2();
                    List<String> sellPayMethods = tuple.getItem3();

                    // Calcular USDT recibido
                    BigDecimal usdtReceived = calculateUsdtReceived(amount, avgBuyPrice);

                    log.info("USDT received from buying with {} {}: {} USDT",
                            amount, route.getCurrencyFromCode(), usdtReceived);

                    // Obtener precio de venta
                    return fetchAverageSellPrice(route, usdtReceived, sellPayMethods)
                            .map(avgSellPrice -> buildExchangeCalculation(
                                    amount,
                                    route.getCurrencyFromCode(),
                                    route.getCurrencyToCode(),
                                    avgBuyPrice,
                                    avgSellPrice,
                                    usdtReceived,
                                    margin
                            ));
                });
    }

    /**
     * Obtiene el precio promedio de compra de USDT.
     */
    private Uni<BigDecimal> fetchAverageBuyPrice(RemittanceRoute route) {
        return paymentMethodPort.findBinancePaymentCodesByCountryCurrency(route.getCountryCurrencyFromId())
                .flatMap(payTypes -> exchangeRateProvider.fetchExchangeRates(
                        USDT,
                        route.getCurrencyFromCode(),
                        TRADE_TYPE_BUY,
                        payTypes,
                        null
                ))
                .map(this::calculateAveragePrice);
    }

    /**
     * Obtiene el precio promedio de venta de USDT.
     */
    private Uni<BigDecimal> fetchAverageSellPrice(
            RemittanceRoute route,
            BigDecimal usdtAmount,
            List<String> payTypes) {

        return exchangeRateProvider.fetchExchangeRates(
                        USDT,
                        route.getCurrencyToCode(),
                        TRADE_TYPE_SELL,
                        payTypes,
                        null,
                        usdtAmount
                )
                .map(this::calculateAveragePrice);
    }

    /**
     * Calcula el precio promedio de los primeros N precios.
     */
    private BigDecimal calculateAveragePrice(List<ExchangeRate> rates) {
        if (rates == null || rates.isEmpty()) {
            throw new IllegalStateException("No exchange rates available from provider");
        }

        BigDecimal sum = rates.stream()
                .limit(TOP_PRICES_LIMIT)
                .map(ExchangeRate::price)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int count = Math.min(rates.size(), TOP_PRICES_LIMIT);
        return sum.divide(BigDecimal.valueOf(count), SCALE, RoundingMode.HALF_UP);
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
