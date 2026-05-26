package org.walrex.domain.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.walrex.application.port.input.UpdateExchangeRatesUseCase;
import org.walrex.application.port.output.ExchangeRateCachePort;
import org.walrex.application.port.output.RemittanceRouteOutputPort;
import org.walrex.domain.model.ExchangeRateCache;
import org.walrex.domain.model.ExchangeRateRouteInfo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Calcula tasas derivadas que no vienen directamente de un proveedor.
 *
 * EUR/VES = PEN/VES (Binance P2P) × EUR/PEN (AstroPay)
 */
@Slf4j
@ApplicationScoped
public class DerivedExchangeRateService {

    @Inject
    RemittanceRouteOutputPort routeOutputPort;

    @Inject
    ExchangeRateCachePort cachePort;

    @Inject
    UpdateExchangeRatesUseCase updateExchangeRatesUseCase;

    public Uni<Void> updateEurVes() {
        return routeOutputPort.findAllActiveExchangeRateRoutes()
                .chain(routes -> {
                    Optional<ExchangeRateRouteInfo> eurVesRoute = routes.stream()
                            .filter(r -> "EUR".equalsIgnoreCase(r.getCurrencyFromCode())
                                      && "VES".equalsIgnoreCase(r.getCurrencyToCode()))
                            .findFirst();

                    Optional<ExchangeRateRouteInfo> eurPenRoute = routes.stream()
                            .filter(r -> "EUR".equalsIgnoreCase(r.getCurrencyFromCode())
                                      && "PEN".equalsIgnoreCase(r.getCurrencyToCode()))
                            .findFirst();

                    Optional<ExchangeRateRouteInfo> penVesRoute = routes.stream()
                            .filter(r -> "PEN".equalsIgnoreCase(r.getCurrencyFromCode())
                                      && "VES".equalsIgnoreCase(r.getCurrencyToCode()))
                            .findFirst();

                    if (eurVesRoute.isEmpty() || eurPenRoute.isEmpty() || penVesRoute.isEmpty()) {
                        log.warn("[DerivedRate] Rutas necesarias para EUR/VES no encontradas: eurVes={} eurPen={} penVes={}",
                                eurVesRoute.isPresent(), eurPenRoute.isPresent(), penVesRoute.isPresent());
                        return Uni.createFrom().voidItem();
                    }

                    LocalDate today = LocalDate.now();
                    String eurPenKey = ExchangeRateCache.generateCacheKey(
                            eurPenRoute.get().getCountryFromCode(), "EUR",
                            eurPenRoute.get().getCountryToCode(), "PEN",
                            today);
                    String penVesKey = ExchangeRateCache.generateCacheKey(
                            penVesRoute.get().getCountryFromCode(), "PEN",
                            penVesRoute.get().getCountryToCode(), "VES",
                            today);

                    return Uni.combine().all()
                            .unis(cachePort.get(eurPenKey), cachePort.get(penVesKey))
                            .asTuple()
                            .chain(tuple -> {
                                Optional<ExchangeRateCache> eurPenCache = tuple.getItem1();
                                Optional<ExchangeRateCache> penVesCache = tuple.getItem2();

                                if (eurPenCache.isEmpty()) {
                                    log.warn("[DerivedRate] EUR/PEN no está en caché (key={}), omitiendo EUR/VES", eurPenKey);
                                    return Uni.createFrom().voidItem();
                                }
                                if (penVesCache.isEmpty()) {
                                    log.warn("[DerivedRate] PEN/VES no está en caché (key={}), omitiendo EUR/VES", penVesKey);
                                    return Uni.createFrom().voidItem();
                                }

                                BigDecimal eurPenRate = eurPenCache.get().getRate();
                                BigDecimal penVesRate = penVesCache.get().getRate();
                                // EUR/VES = cuántos VES por 1 EUR
                                // Si PEN/VES = 14 (1 PEN → 14 VES) y EUR/PEN = 3.8 (1 EUR → 3.8 PEN)
                                // entonces EUR/VES = 14 × 3.8 = 53.2
                                BigDecimal eurVesRate = penVesRate.multiply(eurPenRate)
                                        .setScale(5, RoundingMode.HALF_UP);

                                log.info("[DerivedRate] EUR/VES = PEN/VES({}) × EUR/PEN({}) = {}",
                                        penVesRate, eurPenRate, eurVesRate);

                                return updateExchangeRatesUseCase.saveRateForRoute(eurVesRoute.get(), eurVesRate);
                            });
                })
                .onFailure().invoke(e ->
                        log.error("[DerivedRate] Error calculando EUR/VES: {}", e.getMessage(), e))
                .onFailure().recoverWithNull();
    }
}
