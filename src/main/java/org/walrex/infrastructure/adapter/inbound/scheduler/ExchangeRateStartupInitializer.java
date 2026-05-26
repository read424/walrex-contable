package org.walrex.infrastructure.adapter.inbound.scheduler;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.keys.ReactiveKeyCommands;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.impl.ContextInternal;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.walrex.application.port.input.UpdateAstroPayExchangeRateUseCase;
import org.walrex.application.port.input.UpdateExchangeRatesUseCase;

/**
 * Inicializa las tasas de cambio al arrancar la aplicación:
 *
 *  1. Limpia claves exchange_rate:* de Redis (datos del día anterior u obsoletos).
 *  2. Fetch inicial de rutas BINANCE vía Binance P2P.
 *  3. Fetch inicial de rutas ASTROPAY vía AstroPay.
 *
 * Se ejecuta con un delay de 5 s para garantizar que Hibernate Reactive
 * y los WebSocket adapters (Finnhub 2 s, Binance 3 s) hayan iniciado.
 */
@Slf4j
@ApplicationScoped
public class ExchangeRateStartupInitializer {

    private static final String KEY_PATTERN = "exchange_rate:*";

    @Inject
    Vertx vertx;

    @Inject
    UpdateExchangeRatesUseCase updateExchangeRatesUseCase;

    @Inject
    UpdateAstroPayExchangeRateUseCase updateAstroPayExchangeRateUseCase;

    @Inject
    ReactiveRedisDataSource redisDataSource;

    private ReactiveKeyCommands<String> keyCommands;

    @PostConstruct
    void init() {
        this.keyCommands = redisDataSource.key(String.class);
    }

    void onStart(@Observes StartupEvent event) {
        vertx.setTimer(5000, id -> {
            Context safeCtx = ((ContextInternal) vertx.getOrCreateContext()).duplicate();
            VertxContextSafetyToggle.setContextSafe(safeCtx, true);
            safeCtx.runOnContext(v -> initialize());
        });
    }

    // ── Flujo principal ───────────────────────────────────────────────────────

    private void initialize() {
        log.info("[ExchangeRateInit] === Iniciando carga inicial de tasas de cambio ===");

        // Binance corre primero. Al completar, sessionFactory.withTransaction() deja la sesión
        // cerrada en el contexto Vert.x. Si AstroPay corriera en el mismo contexto, @WithSession
        // reusaría esa sesión cerrada y fallaría. Solución: nuevo contexto duplicado para AstroPay.
        clearExchangeRateCache()
                .chain(this::fetchBinanceRates)
                .subscribe().with(
                        __ -> runAstroPayInFreshContext(),
                        e -> {
                            log.error("[ExchangeRateInit] Error durante carga BINANCE: {}", e.getMessage(), e);
                            runAstroPayInFreshContext();
                        }
                );
    }

    private void runAstroPayInFreshContext() {
        Context freshCtx = ((ContextInternal) vertx.getOrCreateContext()).duplicate();
        VertxContextSafetyToggle.setContextSafe(freshCtx, true);
        freshCtx.runOnContext(v ->
                fetchAstroPayRates().subscribe().with(
                        __ -> log.info("[ExchangeRateInit] === Carga inicial completada ==="),
                        e -> log.error("[ExchangeRateInit] Error durante carga ASTROPAY: {}", e.getMessage(), e)
                )
        );
    }

    // ── Pasos ─────────────────────────────────────────────────────────────────

    private Uni<Void> clearExchangeRateCache() {
        return keyCommands.keys(KEY_PATTERN)
                .chain(keys -> {
                    if (keys == null || keys.isEmpty()) {
                        log.info("[ExchangeRateInit] Redis sin claves {}", KEY_PATTERN);
                        return Uni.createFrom().voidItem();
                    }
                    log.info("[ExchangeRateInit] Limpiando {} claves Redis {}", keys.size(), KEY_PATTERN);
                    return keyCommands.del(keys.toArray(new String[0])).replaceWithVoid();
                })
                .onFailure().invoke(e ->
                        log.error("[ExchangeRateInit] Error limpiando Redis: {}", e.getMessage()))
                .onFailure().recoverWithNull();
    }

    private Uni<Void> fetchBinanceRates() {
        log.info("[ExchangeRateInit] Fetching tasas BINANCE (Binance P2P)...");
        return updateExchangeRatesUseCase.updateExchangeRates()
                .invoke(update -> log.info("[ExchangeRateInit] BINANCE completado — {} pares",
                        update.ratesByPair().size()))
                .replaceWithVoid()
                .onFailure().invoke(e ->
                        log.error("[ExchangeRateInit] Error en fetch BINANCE: {}", e.getMessage()))
                .onFailure().recoverWithNull();
    }

    private Uni<Void> fetchAstroPayRates() {
        log.info("[ExchangeRateInit] Fetching tasas ASTROPAY...");
        return updateAstroPayExchangeRateUseCase.updateRatesForActiveRoutes()
                .invoke(__ -> log.info("[ExchangeRateInit] ASTROPAY completado"))
                .onFailure().invoke(e ->
                        log.error("[ExchangeRateInit] Error en fetch ASTROPAY: {}", e.getMessage()))
                .onFailure().recoverWithNull();
    }
}
