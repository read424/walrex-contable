package org.walrex.infrastructure.adapter.inbound.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.value.ReactiveValueCommands;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.WebSocket;
import io.vertx.core.http.WebSocketConnectOptions;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.walrex.application.port.input.UpdateExchangeRatesUseCase;
import org.walrex.infrastructure.adapter.inbound.websocket.dto.BinanceAggTradeMessage;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Estrategia "Shadowing": escucha el stream spot de Binance como sensor de volatilidad.
 *
 * Flujo:
 *   1. Conecta a wss://stream.binance.com/ws/{symbol}@aggTrade
 *   2. Monitorea variación de precio respecto al último precio de referencia.
 *   3. Si la variación supera el umbral (default 1.5%) → verifica cooldown en Redis.
 *   4. Si el cooldown expiró → dispara UpdateExchangeRatesUseCase (Binance P2P fetch
 *      + cross rate + escritura en Redis exchange_rate:* + BD + FCM).
 *   5. Resetea precio de referencia y activa cooldown en Redis (default 5 min).
 */
@Slf4j
@ApplicationScoped
public class BinanceSpotStreamAdapter {

    private static final String WS_HOST = "stream.binance.com";
    private static final int WS_PORT = 443;
    private static final String COOLDOWN_KEY = "exchange_rate:trigger:cooldown";

    @ConfigProperty(name = "binance.spot.stream.symbol", defaultValue = "btcusdt")
    String symbol;

    @ConfigProperty(name = "binance.spot.stream.price-change-threshold", defaultValue = "1.5")
    BigDecimal priceChangeThreshold;

    @ConfigProperty(name = "binance.spot.stream.cooldown-minutes", defaultValue = "5")
    long cooldownMinutes;

    @ConfigProperty(name = "binance.spot.stream.reconnect-delay-ms", defaultValue = "5000")
    long reconnectDelayMs;

    @Inject
    Vertx vertx;

    @Inject
    UpdateExchangeRatesUseCase updateExchangeRatesUseCase;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    ReactiveRedisDataSource redisDataSource;

    private ReactiveValueCommands<String, String> valueCommands;

    private final AtomicBoolean shutdownRequested = new AtomicBoolean(false);
    private final AtomicReference<BigDecimal> referencePrice = new AtomicReference<>();

    private volatile HttpClient httpClient;

    @PostConstruct
    void init() {
        this.valueCommands = redisDataSource.value(String.class, String.class);
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    void onStart(@Observes StartupEvent event) {
        // Delay de 3s para dar tiempo al contexto Vert.x de estar listo
        vertx.setTimer(3000, id -> connect());
    }

    void onStop(@Observes ShutdownEvent event) {
        shutdownRequested.set(true);
        if (httpClient != null) httpClient.close();
    }

    // ── Conexión WebSocket ────────────────────────────────────────────────────

    private void connect() {
        String uri = "/ws/" + symbol.toLowerCase() + "@aggTrade";
        log.info("[BinanceSpot] Conectando a {}{}...", WS_HOST, uri);

        httpClient = vertx.createHttpClient(new HttpClientOptions()
                .setSsl(true)
                .setDefaultHost(WS_HOST)
                .setDefaultPort(WS_PORT));

        httpClient.webSocket(new WebSocketConnectOptions()
                        .setHost(WS_HOST)
                        .setPort(WS_PORT)
                        .setURI(uri)
                        .setSsl(true))
                .onSuccess(this::onConnected)
                .onFailure(e -> {
                    log.error("[BinanceSpot] Conexión fallida: {} — reintento en {}ms",
                            e.getMessage(), reconnectDelayMs);
                    scheduleReconnect();
                });
    }

    private void onConnected(WebSocket ws) {
        log.info("[BinanceSpot] Conectado — escuchando {}@aggTrade (umbral={}%)",
                symbol, priceChangeThreshold);

        ws.textMessageHandler(this::handleMessage);
        ws.closeHandler(v -> {
            log.warn("[BinanceSpot] Conexión cerrada — reintento en {}ms", reconnectDelayMs);
            scheduleReconnect();
        });
        ws.exceptionHandler(e ->
                log.error("[BinanceSpot] Error en WebSocket: {}", e.getMessage(), e));
    }

    // ── Procesamiento de mensajes ─────────────────────────────────────────────

    private void handleMessage(String raw) {
        try {
            BinanceAggTradeMessage trade = objectMapper.readValue(raw, BinanceAggTradeMessage.class);
            if (trade.getPrice() == null) return;

            BigDecimal currentPrice = new BigDecimal(trade.getPrice());
            BigDecimal reference = referencePrice.get();

            if (reference == null) {
                referencePrice.set(currentPrice);
                log.info("[BinanceSpot] {} precio de referencia inicial={}", symbol, currentPrice);
                return;
            }

            BigDecimal changePct = currentPrice.subtract(reference)
                    .divide(reference, 6, RoundingMode.HALF_UP)
                    .abs()
                    .multiply(BigDecimal.valueOf(100));

            if (changePct.compareTo(priceChangeThreshold) < 0) return;

            log.info("[BinanceSpot] {} variación={}% supera umbral={}% — verificando cooldown",
                    symbol, changePct.setScale(4, RoundingMode.HALF_UP), priceChangeThreshold);

            triggerIfCooldownExpired(currentPrice, changePct);

        } catch (Exception e) {
            log.error("[BinanceSpot] Error procesando mensaje: {}", e.getMessage());
        }
    }

    // ── Cooldown + Trigger ────────────────────────────────────────────────────

    private void triggerIfCooldownExpired(BigDecimal currentPrice, BigDecimal changePct) {
        valueCommands.get(COOLDOWN_KEY)
                .subscribe().with(
                        existing -> {
                            if (existing != null) {
                                log.debug("[BinanceSpot] Cooldown activo — trigger omitido");
                                return;
                            }
                            // Cooldown expirado: resetear referencia, activar cooldown y disparar
                            referencePrice.set(currentPrice);
                            activateCooldown();
                            triggerUpdate(changePct);
                        },
                        e -> log.error("[BinanceSpot] Error verificando cooldown en Redis: {}", e.getMessage())
                );
    }

    private void activateCooldown() {
        long ttlSeconds = cooldownMinutes * 60;
        valueCommands.setex(COOLDOWN_KEY, ttlSeconds, "1")
                .subscribe().with(
                        __ -> log.debug("[BinanceSpot] Cooldown activado por {} min", cooldownMinutes),
                        e -> log.error("[BinanceSpot] Error activando cooldown en Redis: {}", e.getMessage())
                );
    }

    private void triggerUpdate(BigDecimal changePct) {
        log.info("[BinanceSpot] Disparando actualización Binance P2P — variación acumulada={}%",
                changePct.setScale(4, RoundingMode.HALF_UP));

        updateExchangeRatesUseCase.updateExchangeRates()
                .subscribe().with(
                        update -> log.info("[BinanceSpot] Actualización completada — {} pares procesados",
                                update.ratesByPair().size()),
                        e -> log.error("[BinanceSpot] Error en actualización de tasas P2P: {}", e.getMessage(), e)
                );
    }

    // ── Reconexión ────────────────────────────────────────────────────────────

    private void scheduleReconnect() {
        if (shutdownRequested.get()) return;
        vertx.setTimer(reconnectDelayMs, id -> connect());
    }
}
