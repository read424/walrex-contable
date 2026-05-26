package org.walrex.domain.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.walrex.application.port.input.UpdateAstroPayExchangeRateUseCase;
import org.walrex.application.port.input.UpdateExchangeRatesUseCase;
import org.walrex.application.port.output.AstroPayPort;
import org.walrex.application.port.output.RemittanceRouteOutputPort;
import org.walrex.domain.model.AstroPayRate;
import org.walrex.domain.model.ExchangeRateRouteInfo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Slf4j
@ApplicationScoped
public class AstroPayExchangeRateService implements UpdateAstroPayExchangeRateUseCase {

    @ConfigProperty(name = "astropay.margin-percent", defaultValue = "6")
    BigDecimal marginPercent;

    @Inject
    RemittanceRouteOutputPort routeOutputPort;

    @Inject
    AstroPayPort astroPayPort;

    @Inject
    UpdateExchangeRatesUseCase updateExchangeRatesUseCase;

    // ── API pública ───────────────────────────────────────────────────────────

    @Override
    public Uni<Void> updateRatesForActiveRoutes() {
        return routeOutputPort.findActiveRoutesByProvider("ASTROPAY")
                .chain(routes -> {
                    if (routes.isEmpty()) {
                        log.info("[AstroPayExchangeRate] Sin rutas ASTROPAY activas");
                        return Uni.createFrom().voidItem();
                    }
                    log.info("[AstroPayExchangeRate] Actualizando {} rutas ASTROPAY", routes.size());
                    return executeSequentially(routes.stream()
                            .map(this::fetchAndSaveForRoute)
                            .toList());
                });
    }

    @Override
    public Uni<Void> updateRatesForCurrency(String currencyCode) {
        return routeOutputPort.findActiveRoutesByProvider("ASTROPAY")
                .chain(routes -> {
                    List<Uni<Void>> affected = routes.stream()
                            .filter(r -> currencyCode.equalsIgnoreCase(r.getCurrencyFromCode())
                                      || currencyCode.equalsIgnoreCase(r.getCurrencyToCode()))
                            .map(this::fetchAndSaveForRoute)
                            .toList();
                    if (affected.isEmpty()) {
                        log.info("[AstroPayExchangeRate] No hay rutas ASTROPAY para moneda {}", currencyCode);
                        return Uni.createFrom().voidItem();
                    }
                    log.info("[AstroPayExchangeRate] Refrescando {} rutas ASTROPAY para {}", affected.size(), currencyCode);
                    return executeSequentially(affected);
                });
    }

    @Override
    public Uni<Void> saveRateForPair(String currencyFrom, String currencyTo, AstroPayRate rate) {
        return routeOutputPort.findActiveRoutesByProvider("ASTROPAY")
                .chain(routes -> routes.stream()
                        .filter(r -> r.getCurrencyFromCode().equalsIgnoreCase(currencyFrom)
                                && r.getCurrencyToCode().equalsIgnoreCase(currencyTo))
                        .findFirst()
                        .map(route -> applyMarginAndSave(route, rate))
                        .orElseGet(() -> {
                            log.warn("[AstroPayExchangeRate] Ruta no encontrada para {}→{}", currencyFrom, currencyTo);
                            return Uni.createFrom().voidItem();
                        }));
    }

    // ── Lógica interna ────────────────────────────────────────────────────────

    private Uni<Void> fetchAndSaveForRoute(ExchangeRateRouteInfo route) {
        String from = route.getCurrencyFromCode();
        String to = route.getCurrencyToCode();

        return astroPayPort.getExchangeRate(from, to)
                .chain(rate -> applyMarginAndSave(route, rate))
                .onFailure().invoke(e ->
                        log.error("[AstroPayExchangeRate] Error actualizando {}→{}: {}", from, to, e.getMessage()))
                .onFailure().recoverWithNull();
    }

    private Uni<Void> applyMarginAndSave(ExchangeRateRouteInfo route, AstroPayRate rate) {
        // Divisor = 1 + (marginPercent / 100). Ejemplo: 6 → 1.06
        // Dividir (no multiplicar) para dar menos al cliente y capturar el margen.
        BigDecimal divisor = BigDecimal.ONE.add(marginPercent.divide(BigDecimal.valueOf(100), 5, RoundingMode.HALF_UP));
        BigDecimal marginRate = rate.getExchange().divide(divisor, 5, RoundingMode.HALF_UP);

        log.info("[AstroPayExchangeRate] {}→{} exchange={} margin={}% divisor={} marginRate={}",
                route.getCurrencyFromCode(), route.getCurrencyToCode(),
                rate.getExchange(), marginPercent, divisor, marginRate);

        return updateExchangeRatesUseCase.saveRateForRoute(route, marginRate);
    }

    // Ejecución secuencial para evitar conflictos de sesión en Hibernate Reactive
    private Uni<Void> executeSequentially(List<Uni<Void>> unis) {
        Uni<Void> chain = Uni.createFrom().voidItem();
        for (Uni<Void> uni : unis) {
            chain = chain.chain(() -> uni);
        }
        return chain;
    }
}
