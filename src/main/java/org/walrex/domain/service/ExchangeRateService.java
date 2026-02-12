package org.walrex.domain.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.walrex.application.port.input.UpdateExchangeRatesUseCase;
import org.walrex.application.port.output.ExchangeRateCachePort;
import org.walrex.application.port.output.ExchangeRateProviderPort;
import org.walrex.application.port.output.PaymentMethodQueryPort;
import org.walrex.application.port.output.PriceExchangeOutputPort;
import org.walrex.application.port.output.PushNotificationPort;
import org.walrex.application.port.output.RemittanceRouteOutputPort;
import org.walrex.domain.model.ExchangeRate;
import org.walrex.domain.model.ExchangeRateCache;
import org.walrex.domain.model.ExchangeRateRouteInfo;
import org.walrex.domain.model.RemittanceRoute;
import org.walrex.domain.model.ExchangeRateUpdate;
import org.walrex.domain.model.RouteRates;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
public class ExchangeRateService implements UpdateExchangeRatesUseCase {

    private static final BigDecimal CHANGE_THRESHOLD_PERCENT = BigDecimal.valueOf(0.8); // ±0.5%
    private static final Duration CACHE_TTL = Duration.ofHours(12); // 12 horas

    @Inject
    ExchangeRateProviderPort exchangeRateProvider;

    @Inject
    PriceExchangeOutputPort priceExchangePort;

    @Inject
    RemittanceRouteOutputPort remittanceRoutePort;

    @Inject
    PaymentMethodQueryPort paymentMethodQueryPort;

    @Inject
    ExchangeRateCachePort cachePort;

    @Inject
    PushNotificationPort pushNotificationPort;

    /**
     * Actualiza todas las tasas de cambio basado en las rutas de remesas configuradas
     *
     * @return Uni con todas las tasas actualizadas
     */
    @Override
    public Uni<ExchangeRateUpdate> updateExchangeRates() {
        log.info("Starting exchange rate update for remittances");

        return remittanceRoutePort.findAllActiveExchangeRateRoutes()
                .onFailure().invoke(error ->
                        log.error("=== [ERROR] Failed to fetch active routes ===", error)
                )
                .onItem().invoke(routes ->
                        log.info("=== [ROUTES] Retrieved {} active routes from database ===", routes.size())
                )
                .onItem().transformToUni(routes -> {
                    if (routes.isEmpty()) {
                        log.warn("No active remittance routes found");
                        return Uni.createFrom().item(new ExchangeRateUpdate(Collections.emptyMap()));
                    }

                    log.info("Found {} active remittance routes", routes.size());

                    // Agrupar por país + par de monedas (countryFrom:currencyFrom/intermediaryAsset/currencyTo)
                    // Ejemplo: EC:USD/USDT/VES, PE:PEN/USDT/VES, PE:USD/USDT/VES, US:USD/USDT/VES
                    Map<String, ExchangeRateRouteInfo> uniquePairs = routes.stream()
                            .collect(Collectors.toMap(
                                    route -> route.getCountryFromCode() + ":" +
                                            route.getCurrencyFromCode() + "/" +
                                            route.getIntermediaryAsset() + "/" +
                                            route.getCurrencyToCode(),
                                    route -> route,
                                    (r1, r2) -> r1 // Si hay duplicados, tomar el primero
                            ));

                    log.info("Processing {} unique currency pairs", uniquePairs.size());
                    uniquePairs.forEach((key, route) ->
                            log.info("=== [PAIR] {} | ccFromId:{} ccToId:{} | currFromId:{} currToId:{} ===",
                                    key, route.getCountryCurrencyFromId(), route.getCountryCurrencyToId(),
                                    route.getCurrencyFromId(), route.getCurrencyToId())
                    );

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
                                calculateAverageRates(update);

                                // LOG para debug: mostrar el contenido de ExchangeRateUpdate
                                log.info("=== ExchangeRateUpdate: {} ===", update);

                                return update;
                            })
                            .onItem().transformToUni(update->
                                saveAverageRates(update).replaceWith(update)
                            );
                });
    }

    /**
     * Consulta tasas BUY y SELL para un par de monedas específico
     */
    private Uni<Map.Entry<String, RouteRates>> fetchRatesForPair(
            String pairKey,
            ExchangeRateRouteInfo route) {

        Long countryCurrencyFromId = route.getCountryCurrencyFromId();
        Integer currencyFromId = route.getCurrencyFromId();
        Long countryCurrencyToId = route.getCountryCurrencyToId();
        Integer currencyToId = route.getCurrencyToId();
        String asset = route.getIntermediaryAsset();
        String currencyFrom = route.getCurrencyFromCode();
        String currencyTo = route.getCurrencyToCode();

        log.debug("Fetching rates for pair: {} ({}->{}), asset:{}",
                pairKey, currencyFrom, currencyTo, asset);

        // Query payment methods for FROM currency (for BUY USDT with PEN)
        Uni<List<String>> fromPaymentMethods = paymentMethodQueryPort
                .findBinancePaymentCodesByCountryCurrency(countryCurrencyFromId)
                .onItem().invoke(methods -> {
                    if (methods.isEmpty()) {
                        log.warn("No payment methods configured for country_currency {}", countryCurrencyFromId);
                    }
                    log.debug("Payment methods for BUY {}/{}: {}", asset, currencyFrom, methods);
                });

        // Query payment methods for TO currency (for SELL USDT for VES)
        Uni<List<String>> toPaymentMethods = paymentMethodQueryPort
                .findBinancePaymentCodesByCountryCurrency(countryCurrencyToId)
                .onItem().invoke(methods -> {
                    if (methods.isEmpty()) {
                        log.warn("No payment methods configured for country_currency {}", countryCurrencyToId);
                    }
                    log.debug("Payment methods for SELL {}/{}: {}", asset, currencyTo, methods);
                });

        // Fetch BUY rates with FROM payment methods
        Uni<List<ExchangeRate>> buyRates = fromPaymentMethods
                .onItem().transformToUni(payMethods ->
                        fetchRates(asset, currencyFrom, "BUY", payMethods));

        // Fetch SELL rates with TO payment methods
        Uni<List<ExchangeRate>> sellRates = toPaymentMethods
                .onItem().transformToUni(payMethods ->
                        fetchRates(asset, currencyTo, "SELL", payMethods));

        return Uni.combine().all().unis(buyRates, sellRates)
                .asTuple()
                .map(tuple -> {
                    RouteRates rates = new RouteRates(
                            currencyFromId,
                            currencyFrom,
                            currencyToId,
                            currencyTo,
                            countryCurrencyFromId,
                            countryCurrencyToId,
                            route.getCountryFromCode(),
                            route.getCountryToCode(),
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
    private Uni<List<ExchangeRate>> fetchRates(String asset, String fiat, String tradeType, List<String> payTypes) {
        return exchangeRateProvider.fetchExchangeRates(asset, fiat, tradeType, payTypes, null)
                .onFailure().invoke(error ->
                        log.error("Failed to fetch {} {} {} rates with payTypes {}: {}",
                                asset, fiat, tradeType, payTypes, error.getMessage())
                )
                .onFailure().recoverWithItem(Collections.emptyList());
    }

    /**
     * Calcula las mejores tasas disponibles y las registra en logs
     */
    private void calculateBestRates(ExchangeRateUpdate update) {
        update.ratesByPair().forEach((pair, rates) -> {
            String currencyFrom = rates.currencyFromCode();  // PEN
            String currencyTo = rates.currencyToCode();      // VES

            // Mejor tasa para comprar (BUY) - precio más bajo
            // BUY = comprar USDT con currencyFrom (ej: PEN)
            rates.buyRates().stream()
                    .min(Comparator.comparing(ExchangeRate::price))
                    .ifPresent(best ->
                            log.info("Best {} BUY rate: {} {} per USDT (merchant: {})",
                                    pair, best.price(), currencyFrom, best.merchantNickName())
                    );

            // Mejor tasa para vender (SELL) - precio más alto
            // SELL = vender USDT por currencyTo (ej: VES)
            rates.sellRates().stream()
                    .max(Comparator.comparing(ExchangeRate::price))
                    .ifPresent(best ->
                            log.info("Best {} SELL rate: {} {} per USDT (merchant: {})",
                                    pair, best.price(), currencyTo, best.merchantNickName())
                    );
        });

        // Calcular tasas cruzadas entre pares
        calculateCrossRates(update);
    }

    /**
     * Calcula el promedio de las 5 mejores tasas para BUY y SELL y las registra en logs
     */
    private void calculateAverageRates(ExchangeRateUpdate update) {
        update.ratesByPair().forEach((pair, rates) -> {
            String currencyFrom = rates.currencyFromCode();  // PEN
            String currencyTo = rates.currencyToCode();      // VES

            // Calcular promedio de las 5 mejores tasas BUY (precios más bajos)
            // BUY = comprar USDT con currencyFrom (ej: PEN)
            if (!rates.buyRates().isEmpty()) {
                List<ExchangeRate> top5Buy = rates.buyRates().stream()
                        .sorted(Comparator.comparing(ExchangeRate::price))
                        .limit(5)
                        .toList();

                BigDecimal sumBuy = top5Buy.stream()
                        .map(ExchangeRate::price)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal avgBuy = sumBuy.divide(
                        BigDecimal.valueOf(top5Buy.size()),
                        5,
                        RoundingMode.HALF_UP
                );

                log.info("Average {} BUY rate (top 5): {} {} per USDT (from {} rates)",
                        pair, avgBuy, currencyFrom, top5Buy.size());
            }

            // Calcular promedio de las 5 mejores tasas SELL (precios más altos)
            // SELL = vender USDT por currencyTo (ej: VES)
            if (!rates.sellRates().isEmpty()) {
                List<ExchangeRate> top5Sell = rates.sellRates().stream()
                        .sorted(Comparator.comparing(ExchangeRate::price).reversed())
                        .limit(5)
                        .toList();

                BigDecimal sumSell = top5Sell.stream()
                        .map(ExchangeRate::price)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal avgSell = sumSell.divide(
                        BigDecimal.valueOf(top5Sell.size()),
                        5,
                        RoundingMode.HALF_UP
                );

                log.info("Average {} SELL rate (top 5): {} {} per USDT (from {} rates)",
                        pair, avgSell, currencyTo, top5Sell.size());
            }
        });
    }

    /**
     * Calcula tasas cruzadas entre diferentes pares usando un activo intermediario
     * Por ejemplo: PEN -> USDT -> VES
     */
    private void calculateCrossRates(ExchangeRateUpdate update) {
        List<String> fiatCurrencies = update.ratesByPair().values().stream()
                .map(RouteRates::currencyFromCode)
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
                .filter(rates -> rates.currencyFromCode().equals(fiat))
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
     * Guarda las tasas cruzadas de cada par con margen del 5%
     * IMPORTANTE: Ejecuta secuencialmente para evitar conflictos de sesión en Hibernate Reactive
     */
    private Uni<Void> saveAverageRates(ExchangeRateUpdate update) {
        log.info("=== [SAVE AVERAGE RATES START] Thread: {} | Pairs to process: {} ===",
                Thread.currentThread().getName(), update.ratesByPair().size());

        LocalDate today = LocalDate.now();

        // Crear lista de operaciones de guardado para cada par
        List<Uni<Integer>> saveOperations = update.ratesByPair().values().stream()
                .flatMap(rates -> {
                    String countryFromCode = rates.countryFromCode();
                    String countryToCode = rates.countryToCode();
                    Integer ccFromId = rates.countryCurrencyFromId().intValue();  // ID country_currencies origen
                    Integer ccToId = rates.countryCurrencyToId().intValue();      // ID country_currencies destino
                    String currencyFrom = rates.currencyFromCode();   // PEN
                    String currencyTo = rates.currencyToCode();       // VES

                    log.info("=== [SAVE PREP] Route: {} -> {} | buyRates:{} sellRates:{} ===",
                            currencyFrom, currencyTo, rates.buyRates().size(), rates.sellRates().size());

                    // Calcular promedios
                    BigDecimal avgBuy = calculateTop5Average(rates.buyRates(), true);   // Promedio compra USDT con PEN
                    BigDecimal avgSell = calculateTop5Average(rates.sellRates(), false); // Promedio venta USDT por VES

                    log.info("=== [AVERAGES] BUY USDT/{}: {} | SELL USDT/{}: {} ===",
                            currencyFrom, avgBuy, currencyTo, avgSell);

                    // --- DEFENSA CONTRA CÁLCULOS CON TASAS INVÁLIDAS/CERO ---
                    if (avgBuy.compareTo(BigDecimal.ZERO) <= 0 || avgSell.compareTo(BigDecimal.ZERO) <= 0) {
                        log.warn("=== [SKIP] Skipping {}->{} due to zero or missing rates (buy: {}, sell: {}) ===",
                                currencyFrom, currencyTo, avgBuy, avgSell);
                        return Stream.empty(); // Saltar este par
                    }

                    // Calcular tasa cruzada: cuántos VES por cada PEN
                    // Fórmula: precio_venta_VES / precio_compra_PEN
                    BigDecimal crossRate = avgSell.divide(avgBuy, 5, RoundingMode.HALF_UP);

                    // Aplicar margen de ganancia del 5%
                    // DIVIDIR (no multiplicar) para dar MENOS al cliente y ganar el 5%
                    // Ejemplo: si crossRate = 14.3, marginRate = 14.3 / 1.05 = 13.62 (cliente recibe menos, nosotros ganamos)
                    BigDecimal marginRate = crossRate.divide(BigDecimal.valueOf(1.065), 5, RoundingMode.HALF_UP);

                    log.info("=== [CROSS RATE] {}->{} | Without margin: {} | With 5% margin: {} ===",
                            currencyFrom, currencyTo, crossRate, marginRate);

                    // Verificar caché y decidir si guardar en BD
                    // Se pasan ccFromId/ccToId (country_currencies IDs) para que cada país
                    // tenga su propio registro en price_exchange
                    return Stream.of(saveRateWithCache(
                            countryFromCode,
                            countryToCode,
                            ccFromId,
                            currencyFrom,
                            ccToId,
                            currencyTo,
                            rates.countryCurrencyFromId(),
                            rates.countryCurrencyToId(),
                            marginRate,
                            today
                    ));
                })
                .toList();

        log.info("=== [SAVE OPS] Total save operations created: {} ===", saveOperations.size());

        if (saveOperations.isEmpty()) {
            log.warn("No rates to save");
            return Uni.createFrom().voidItem();
        }

        // Ejecutar las persistencias SECUENCIALMENTE para evitar conflictos de sesión
        Uni<List<Integer>> chain = Uni.createFrom().item(new java.util.ArrayList<Integer>());

        int opIndex = 0;
        for (Uni<Integer> saveOp : saveOperations) {
            final int currentIndex = opIndex++;
            chain = chain.onItem().transformToUni(list ->
                saveOp.onItem().invoke(id ->
                        log.info("=== [SAVE CHAIN] Operation {} completed with ID: {} ===", currentIndex, id)
                ).onItem().transform(id -> {
                    list.add(id);
                    return list;
                })
            );
        }

        return chain.onItem().invoke(savedIds ->
                log.info("=== [SAVE COMPLETE] Saved {} average rates | IDs: {} ===", savedIds.size(), savedIds)
        ).replaceWithVoid();
    }

    /**
     * Calcula el promedio de las 5 mejores tasas
     */
    private BigDecimal calculateTop5Average(List<ExchangeRate> rates, boolean isBuy) {
        if (rates == null || rates.isEmpty()) {
            return BigDecimal.ZERO;
        }

        List<ExchangeRate> sorted = rates.stream()
                .sorted(isBuy
                        ? Comparator.comparing(ExchangeRate::price)           // BUY: precios más bajos
                        : Comparator.comparing(ExchangeRate::price).reversed()) // SELL: precios más altos
                .limit(5)
                .toList();

        BigDecimal sum = sorted.stream()
                .map(ExchangeRate::price)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return sum.divide(BigDecimal.valueOf(sorted.size()), 5, RoundingMode.HALF_UP);
    }

    /**
     * Guarda una tasa con verificación de caché para evitar escrituras innecesarias en BD.
     *
     * Lógica:
     * 1. Genera clave de caché
     * 2. Consulta caché
     * 3. Si no existe en caché → Guardar en Redis + BD
     * 4. Si existe en caché:
     *    - Calcular diferencia porcentual
     *    - Si dentro de ±0.5% → Solo actualizar TTL en Redis
     *    - Si excede ±0.5% → Guardar en Redis + BD
     */
    private Uni<Integer> saveRateWithCache(
            String countryFromCode,
            String countryToCode,
            Integer currencyFromId,
            String currencyFrom,
            Integer currencyToId,
            String currencyTo,
            Long countryCurrencyFromId,
            Long countryCurrencyToId,
            BigDecimal newRate,
            LocalDate date) {

        String cacheKey = ExchangeRateCache.generateCacheKey(
                countryFromCode, currencyFrom, countryToCode, currencyTo, date);

        log.info("=== [CACHE CHECK] Key: {} | NewRate: {} ===", cacheKey, newRate);

        return cachePort.get(cacheKey)
                .flatMap(cachedOptional -> {
                    if (cachedOptional.isEmpty()) {
                        // No existe en caché → Guardar en Redis + BD
                        log.info("=== [CACHE MISS] {}/{} | Saving to Redis + DB ===",
                                currencyFrom, currencyTo);
                        return saveToRedisAndDb(cacheKey, countryFromCode, countryToCode,
                                currencyFrom, currencyTo,
                                countryCurrencyFromId, countryCurrencyToId,
                                currencyFromId, currencyToId, newRate, date)
                                .onItem().invoke(savedId -> {
                                    try {
                                        Map<String, String> fcmData = new HashMap<>();
                                        fcmData.put("type", "EXCHANGE_RATE_NEW");
                                        fcmData.put("title", "Nueva tasa disponible");
                                        fcmData.put("body", "Tasa " + currencyFrom + "/" + currencyTo + " disponible para hoy");
                                        fcmData.put("screen", "exchange_rates");
                                        fcmData.put("screenArgs", "{\"currencyFrom\":\"" + currencyFrom + "\",\"currencyTo\":\"" + currencyTo + "\"}");

                                        pushNotificationPort.sendToAllActiveDevices(fcmData)
                                                .subscribe().with(
                                                        v -> log.info("=== [FCM] Push notification sent for new {}/{} rate ===",
                                                                currencyFrom, currencyTo),
                                                        err -> log.error("=== [FCM ERROR] Failed to send push notification: {} ===",
                                                                err.getMessage())
                                                );
                                    } catch (Exception e) {
                                        log.error("=== [FCM ERROR] Exception sending push for new {}/{} rate: {} ===",
                                                currencyFrom, currencyTo, e.getMessage());
                                    }
                                });
                    }

                    ExchangeRateCache cached = cachedOptional.get();
                    BigDecimal cachedRate = cached.getRate();
                    BigDecimal percentageChange = calculatePercentageChange(newRate, cachedRate);

                    log.info("=== [CACHE HIT] {}/{} | Cached: {} | New: {} | Change: {}% ===",
                            currencyFrom, currencyTo, cachedRate, newRate, percentageChange);

                    if (exceedsThreshold(percentageChange)) {
                        // Excede threshold → Guardar en Redis + BD
                        log.info("=== [THRESHOLD EXCEEDED] {}/{} | Change {}% > {}% | Saving to Redis + DB ===",
                                currencyFrom, currencyTo, percentageChange, CHANGE_THRESHOLD_PERCENT);
                        return saveToRedisAndDb(cacheKey, countryFromCode, countryToCode,
                                currencyFrom, currencyTo,
                                countryCurrencyFromId, countryCurrencyToId,
                                currencyFromId, currencyToId, newRate, date)
                                .onItem().invoke(savedId -> {
                                    try {
                                        Map<String, String> fcmData = new HashMap<>();
                                        fcmData.put("type", "EXCHANGE_RATE_UPDATE");
                                        fcmData.put("title", "Tipo de cambio actualizado");
                                        fcmData.put("body", currencyFrom + "/" + currencyTo + " ha variado un " + percentageChange.abs() + "%");
                                        fcmData.put("screen", "exchange_rates");
                                        fcmData.put("screenArgs", "{\"currencyFrom\":\"" + currencyFrom + "\",\"currencyTo\":\"" + currencyTo + "\"}");

                                        pushNotificationPort.sendToAllActiveDevices(fcmData)
                                                .subscribe().with(
                                                        v -> log.info("=== [FCM] Push notification sent for {}/{} rate change ===",
                                                                currencyFrom, currencyTo),
                                                        err -> log.error("=== [FCM ERROR] Failed to send push notification: {} ===",
                                                                err.getMessage())
                                                );
                                    } catch (Exception e) {
                                        log.error("=== [FCM ERROR] Exception sending push for {}/{} rate change: {} ===",
                                                currencyFrom, currencyTo, e.getMessage());
                                    }
                                });
                    } else {
                        // Dentro del threshold → Solo actualizar TTL en Redis
                        log.info("=== [WITHIN THRESHOLD] {}/{} | Change {}% <= {}% | Updating TTL only ===",
                                currencyFrom, currencyTo, percentageChange, CHANGE_THRESHOLD_PERCENT);
                        return updateCacheTTL(cacheKey, countryFromCode, countryToCode,
                                currencyFrom, currencyTo, newRate);
                    }
                })
                .onFailure().invoke(error ->
                        log.error("=== [CACHE ERROR] {}/{} | Falling back to direct DB save | Error: {} ===",
                                currencyFrom, currencyTo, error.getMessage())
                )
                .onFailure().recoverWithUni(() -> {
                    // Si Redis falla, guardar directo en BD
                    log.warn("=== [CACHE FALLBACK] {}/{} | Saving to DB only ===", currencyFrom, currencyTo);
                    return saveRate(currencyFromId, currencyFrom, currencyToId, currencyTo,
                            newRate, date, "CROSS_RATE");
                });
    }

    /**
     * Guarda la tasa en la base de datos y luego en Redis.
     * IMPORTANTE: Se ejecuta SECUENCIALMENTE (BD primero, Redis después) para evitar
     * problemas de contexto de transacción Hibernate al correr en paralelo con Redis.
     */
    private Uni<Integer> saveToRedisAndDb(
            String cacheKey,
            String countryFromCode,
            String countryToCode,
            String currencyFrom,
            String currencyTo,
            Long countryCurrencyFromId,
            Long countryCurrencyToId,
            Integer currencyFromId,
            Integer currencyToId,
            BigDecimal rate,
            LocalDate date) {

        // Paso 1: Guardar en BD primero (requiere @WithTransaction limpio)
        return saveRate(currencyFromId, currencyFrom, currencyToId, currencyTo,
                rate, date, "CROSS_RATE")
                .onItem().transformToUni(savedId -> {
                    // Paso 2: BD exitosa → actualizar Redis cache
                    log.info("=== [DB OK] {}/{} savedId:{} | Now saving to Redis cache key: {} ===",
                            currencyFrom, currencyTo, savedId, cacheKey);

                    ExchangeRateCache cacheValue = ExchangeRateCache.builder()
                            .rate(rate)
                            .currencyFrom(currencyFrom)
                            .currencyTo(currencyTo)
                            .countryFromCode(countryFromCode)
                            .countryToCode(countryToCode)
                            .updatedAt(OffsetDateTime.now())
                            .build();

                    return cachePort.set(cacheKey, cacheValue, CACHE_TTL)
                            .onFailure().invoke(error ->
                                    log.error("Failed to save to Redis cache: {}", error.getMessage()))
                            .onFailure().recoverWithNull()
                            .replaceWith(savedId);
                });
    }

    /**
     * Actualiza solo el TTL en Redis sin guardar en BD.
     */
    private Uni<Integer> updateCacheTTL(
            String cacheKey,
            String countryFromCode,
            String countryToCode,
            String currencyFrom,
            String currencyTo,
            BigDecimal rate) {

        ExchangeRateCache cacheValue = ExchangeRateCache.builder()
                .rate(rate)
                .currencyFrom(currencyFrom)
                .currencyTo(currencyTo)
                .countryFromCode(countryFromCode)
                .countryToCode(countryToCode)
                .updatedAt(OffsetDateTime.now())
                .build();

        return cachePort.set(cacheKey, cacheValue, CACHE_TTL)
                .replaceWith(0) // Retornar 0 para indicar que no se guardó en BD
                .onFailure().invoke(error ->
                        log.error("Failed to update cache TTL: {}", error.getMessage())
                )
                .onFailure().recoverWithItem(0);
    }

    /**
     * Guarda una tasa en la base de datos
     */
    private Uni<Integer> saveRate(
            Integer baseId,
            String baseCode,
            Integer quoteId,
            String quoteCode,
            BigDecimal price,
            LocalDate date,
            String operationType) {

        log.info("=== [SAVE RATE] {} | {}/{} ({}→{}) | price: {} | date: {} ===",
                operationType, baseCode, quoteCode, baseId, quoteId, price, date);

        return priceExchangePort.upsertRate(baseId, quoteId, price, date)
                .onItem().invoke(savedId ->
                        log.info("=== [SAVED] {} {}/{} | ID: {} ===", operationType, baseCode, quoteCode, savedId)
                )
                .onFailure().invoke(error ->
                        log.error("=== [SAVE FAILED] {} {}/{} | Error: {} ===",
                                operationType, baseCode, quoteCode, error.getMessage(), error)
                )
                .onFailure().recoverWithItem(0);
    }

    /**
     * Calcula la diferencia porcentual entre dos tasas.
     *
     * Fórmula: ((newRate - cachedRate) / cachedRate) * 100
     *
     * @param newRate Nueva tasa calculada
     * @param cachedRate Tasa almacenada en caché
     * @return Diferencia porcentual (puede ser negativa)
     */
    private BigDecimal calculatePercentageChange(BigDecimal newRate, BigDecimal cachedRate) {
        if (cachedRate.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.valueOf(100); // Cambio total si la tasa anterior era 0
        }

        return newRate.subtract(cachedRate)
                .divide(cachedRate, 5, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    /**
     * Verifica si el cambio porcentual excede el threshold de ±0.5%.
     *
     * @param percentageChange Cambio porcentual calculado
     * @return true si excede el threshold, false si está dentro del rango
     */
    private boolean exceedsThreshold(BigDecimal percentageChange) {
        BigDecimal absChange = percentageChange.abs();
        return absChange.compareTo(CHANGE_THRESHOLD_PERCENT) > 0;
    }
}
