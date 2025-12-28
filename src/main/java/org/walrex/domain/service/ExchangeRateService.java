package org.walrex.domain.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.walrex.application.port.output.ExchangeRateProviderPort;
import org.walrex.application.port.output.PriceExchangeOutputPort;
import org.walrex.application.port.output.RemittanceRouteOutputPort;
import org.walrex.domain.model.ExchangeRate;
import org.walrex.domain.model.RemittanceRoute;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Servicio de dominio para gestionar tasas de cambio entre Perú y Venezuela
 *
 * Flujo del negocio:
 * 1. Consultar tasa USDT -> PEN (compra/venta)
 * 2. Consultar tasa USDT -> VES (compra/venta)
 * 3. Calcular promedio de las 5 mejores tasas
 * 4. Persistir promedios en base de datos
 * 5. Calcular tasa cruzada PEN <-> VES
 */
@Slf4j
@ApplicationScoped
public class ExchangeRateService {

    @Inject
    ExchangeRateProviderPort exchangeRateProvider;

    @Inject
    PriceExchangeOutputPort priceExchangePort;

    @Inject
    RemittanceRouteOutputPort remittanceRoutePort;

    /**
     * Actualiza todas las tasas de cambio basado en las rutas de remesas configuradas
     *
     * @return Uni con todas las tasas actualizadas
     */
    public Uni<ExchangeRateUpdate> updateExchangeRates() {
        log.info("Starting exchange rate update for remittances");

        return remittanceRoutePort.findAllActiveRoutes()
                .onItem().transformToUni(routes -> {
                    if (routes.isEmpty()) {
                        log.warn("No active remittance routes found");
                        return Uni.createFrom().item(new ExchangeRateUpdate(Collections.emptyMap()));
                    }

                    log.info("Found {} active remittance routes", routes.size());

                    // Agrupar por par de monedas (intermediaryAsset/currencyToCode) para evitar consultas duplicadas
                    Map<String, RemittanceRoute> uniquePairs = routes.stream()
                            .collect(Collectors.toMap(
                                    route -> route.intermediaryAsset() + "/" + route.currencyToCode(),
                                    route -> route,
                                    (r1, r2) -> r1 // Si hay duplicados, tomar el primero
                            ));

                    log.info("Processing {} unique currency pairs", uniquePairs.size());

                    // Para cada par único, consultar BUY y SELL
                    List<Uni<Map.Entry<String, RouteRates>>> rateUnis = uniquePairs.entrySet().stream()
                            .map(entry -> fetchRatesForPair(entry.getKey(), entry.getValue()))
                            .toList();

                    // Combinar todos los resultados
                    return Uni.combine().all().unis(rateUnis).with(list -> {
                                @SuppressWarnings("unchecked")
                                Map<String, RouteRates> ratesMap = ((List<Map.Entry<String, RouteRates>>) list)
                                        .stream()
                                        .collect(Collectors.toMap(
                                                Map.Entry::getKey,
                                                Map.Entry::getValue
                                        ));

                                ExchangeRateUpdate update = new ExchangeRateUpdate(ratesMap);

                                log.info("Exchange rates updated successfully for {} pairs", ratesMap.size());
                                logRateSummary(update);

                                // Calcular mejor tasa
                                calculateBestRates(update);

                                return update;
                            })
                            .onItem().transformToUni(update ->
                                    // Persistir promedios de las 5 mejores tasas
                                    saveAverageRates(update).replaceWith(update)
                            );
                });
    }

    /**
     * Consulta tasas BUY y SELL para un par de monedas específico
     */
    private Uni<Map.Entry<String, RouteRates>> fetchRatesForPair(
            String pairKey,
            RemittanceRoute route) {

        Integer assetId = route.intermediaryAssetId();
        String asset = route.intermediaryAsset();
        Integer fiatId = route.currencyToId();
        String fiat = route.currencyToCode();

        log.debug("Fetching rates for pair: {} ({}->{}), asset ID:{}, fiat ID:{}",
                pairKey, route.currencyFromCode(), route.currencyToCode(), assetId, fiatId);

        Uni<List<ExchangeRate>> buyRates = fetchRates(asset, fiat, "BUY");
        Uni<List<ExchangeRate>> sellRates = fetchRates(asset, fiat, "SELL");

        return Uni.combine().all().unis(buyRates, sellRates)
                .asTuple()
                .map(tuple -> {
                    RouteRates rates = new RouteRates(
                            assetId,
                            asset,
                            fiatId,
                            fiat,
                            tuple.getItem1(),
                            tuple.getItem2()
                    );
                    return Map.entry(pairKey, rates);
                });
    }

    /**
     * Registra un resumen de las tasas obtenidas
     */
    private void logRateSummary(ExchangeRateUpdate update) {
        update.ratesByPair().forEach((pair, rates) ->
                log.info("Pair {}: {} buy rates, {} sell rates",
                        pair, rates.buyRates().size(), rates.sellRates().size())
        );
    }

    /**
     * Consulta tasas con manejo de errores
     */
    private Uni<List<ExchangeRate>> fetchRates(String asset, String fiat, String tradeType) {
        return exchangeRateProvider.fetchExchangeRates(asset, fiat, tradeType, null, null)
                .onFailure().invoke(error ->
                        log.error("Failed to fetch {} {} {} rates: {}", asset, fiat, tradeType, error.getMessage())
                )
                .onFailure().recoverWithItem(Collections.emptyList());
    }

    /**
     * Calcula las mejores tasas disponibles y las registra en logs
     */
    private void calculateBestRates(ExchangeRateUpdate update) {
        update.ratesByPair().forEach((pair, rates) -> {
            String asset = rates.asset();
            String fiat = rates.fiat();

            // Mejor tasa para comprar (BUY) - precio más bajo
            rates.buyRates().stream()
                    .min(Comparator.comparing(ExchangeRate::price))
                    .ifPresent(best ->
                            log.info("Best {} BUY rate: {} {} per {} (merchant: {})",
                                    pair, best.price(), fiat, asset, best.merchantNickName())
                    );

            // Mejor tasa para vender (SELL) - precio más alto
            rates.sellRates().stream()
                    .max(Comparator.comparing(ExchangeRate::price))
                    .ifPresent(best ->
                            log.info("Best {} SELL rate: {} {} per {} (merchant: {})",
                                    pair, best.price(), fiat, asset, best.merchantNickName())
                    );
        });

        // Calcular tasas cruzadas entre pares
        calculateCrossRates(update);
    }

    /**
     * Calcula tasas cruzadas entre diferentes pares usando un activo intermediario
     * Por ejemplo: PEN -> USDT -> VES
     */
    private void calculateCrossRates(ExchangeRateUpdate update) {
        List<String> fiatCurrencies = update.ratesByPair().values().stream()
                .map(RouteRates::fiat)
                .distinct()
                .toList();

        // Calcular tasas cruzadas para cada par de monedas fiat
        for (int i = 0; i < fiatCurrencies.size(); i++) {
            for (int j = i + 1; j < fiatCurrencies.size(); j++) {
                String fiat1 = fiatCurrencies.get(i);
                String fiat2 = fiatCurrencies.get(j);

                // Buscar tasas para cada moneda
                RouteRates rates1 = findRatesForFiat(update, fiat1);
                RouteRates rates2 = findRatesForFiat(update, fiat2);

                if (rates1 != null && rates2 != null) {
                    calculateCrossRateBetween(fiat1, fiat2, rates1, rates2);
                }
            }
        }
    }

    /**
     * Encuentra las tasas para una moneda fiat específica
     */
    private RouteRates findRatesForFiat(ExchangeRateUpdate update, String fiat) {
        return update.ratesByPair().values().stream()
                .filter(rates -> rates.fiat().equals(fiat))
                .findFirst()
                .orElse(null);
    }

    /**
     * Calcula la tasa cruzada entre dos monedas fiat usando un intermediario
     */
    private void calculateCrossRateBetween(String fiat1, String fiat2, RouteRates rates1, RouteRates rates2) {
        // Calcular fiat1 -> fiat2
        // 1. Comprar asset con fiat1 (mejor precio BUY en fiat1)
        // 2. Vender asset por fiat2 (mejor precio SELL en fiat2)
        rates1.buyRates().stream()
                .min(Comparator.comparing(ExchangeRate::price))
                .ifPresent(buy1 -> {
                    rates2.sellRates().stream()
                            .max(Comparator.comparing(ExchangeRate::price))
                            .ifPresent(sell2 -> {
                                // 1 fiat1 = ? fiat2
                                // 1 fiat1 -> 1/buy1 asset -> (1/buy1) * sell2 fiat2
                                BigDecimal crossRate = BigDecimal.ONE
                                        .divide(buy1.price(), 10, RoundingMode.HALF_UP)
                                        .multiply(sell2.price())
                                        .setScale(4, RoundingMode.HALF_UP);

                                log.info("Cross rate {}->{}:  {} {} per {}",
                                        fiat1, fiat2, crossRate, fiat2, fiat1);
                            });
                });
    }

    /**
     * Guarda los promedios de las 5 mejores tasas de cada par
     * IMPORTANTE: Ejecuta secuencialmente para evitar conflictos de sesión en Hibernate Reactive
     */
    private Uni<Void> saveAverageRates(ExchangeRateUpdate update) {
        LocalDate today = LocalDate.now();

        // Crear lista de operaciones de guardado para cada par
        List<Uni<Integer>> saveOperations = update.ratesByPair().values().stream()
                .flatMap(rates -> {
                    Integer assetId = rates.assetCurrencyId();
                    Integer fiatId = rates.fiatCurrencyId();
                    String asset = rates.asset();
                    String fiat = rates.fiat();

                    // Crear dos operaciones: una para BUY, otra para SELL
                    return java.util.stream.Stream.of(
                            calculateAndSaveAverage(rates.buyRates(), assetId, asset, fiatId, fiat, true, today),
                            calculateAndSaveAverage(rates.sellRates(), assetId, asset, fiatId, fiat, false, today)
                    );
                })
                .toList();

        if (saveOperations.isEmpty()) {
            log.warn("No rates to save");
            return Uni.createFrom().voidItem();
        }

        // Ejecutar las persistencias SECUENCIALMENTE para evitar conflictos de sesión
        // Comenzamos con un Uni inicial que devuelve lista vacía
        Uni<List<Integer>> chain = Uni.createFrom().item(new java.util.ArrayList<Integer>());

        // Concatenamos cada operación de guardado secuencialmente
        for (Uni<Integer> saveOp : saveOperations) {
            chain = chain.onItem().transformToUni(list ->
                saveOp.onItem().transform(id -> {
                    list.add(id);
                    return list;
                })
            );
        }

        return chain.onItem().invoke(savedIds ->
                log.info("Saved {} average rates", savedIds.size())
        ).replaceWithVoid();
    }

    /**
     * Calcula el promedio de las 5 mejores tasas y las guarda
     *
     * @param rates Lista de tasas
     * @param baseId ID de la moneda base en la tabla currencies
     * @param baseCode Código moneda base (para logging)
     * @param quoteId ID de la moneda quote en la tabla currencies
     * @param quoteCode Código moneda quote (para logging)
     * @param isBuy true para BUY (mejores = precios más bajos), false para SELL (mejores = precios más altos)
     * @param date Fecha
     * @return Uni con el ID del registro guardado
     */
    private Uni<Integer> calculateAndSaveAverage(
            List<ExchangeRate> rates,
            Integer baseId,
            String baseCode,
            Integer quoteId,
            String quoteCode,
            boolean isBuy,
            LocalDate date) {

        if (rates == null || rates.isEmpty()) {
            log.warn("No rates available for {}/{} {}", baseCode, quoteCode, isBuy ? "BUY" : "SELL");
            return Uni.createFrom().item(0);
        }

        // Ordenar por precio: ascendente para BUY, descendente para SELL
        List<ExchangeRate> sorted = rates.stream()
                .sorted(isBuy
                        ? Comparator.comparing(ExchangeRate::price)
                        : Comparator.comparing(ExchangeRate::price).reversed())
                .limit(5)  // Tomar las 5 mejores
                .toList();

        // Calcular promedio
        BigDecimal sum = sorted.stream()
                .map(ExchangeRate::price)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal average = sum.divide(
                BigDecimal.valueOf(sorted.size()),
                5,
                RoundingMode.HALF_UP
        );

        log.info("Calculated average for {}/{} (ID:{} -> ID:{}) {}: {} (from {} rates)",
                baseCode, quoteCode, baseId, quoteId, isBuy ? "BUY" : "SELL", average, sorted.size());

        // Guardar promedio usando los IDs de monedas
        return priceExchangePort.upsertRate(baseId, quoteId, average, date)
                .onFailure().invoke(error ->
                        log.error("Failed to save average rate for {}/{}: {}", baseCode, quoteCode, error.getMessage())
                )
                .onFailure().recoverWithItem(0);
    }

    /**
     * Record para encapsular el resultado de la actualización
     */
    public record ExchangeRateUpdate(
            Map<String, RouteRates> ratesByPair
    ) {}

    /**
     * Record para las tasas de un par de monedas específico
     */
    public record RouteRates(
            Integer assetCurrencyId,      // ID de la moneda base (intermediary asset) en la tabla currencies
            String asset,
            Integer fiatCurrencyId,        // ID de la moneda quote (fiat) en la tabla currencies
            String fiat,
            List<ExchangeRate> buyRates,
            List<ExchangeRate> sellRates
    ) {}
}
