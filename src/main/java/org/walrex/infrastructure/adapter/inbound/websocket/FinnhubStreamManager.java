package org.walrex.infrastructure.adapter.inbound.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.impl.ContextInternal;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.WebSocket;
import io.vertx.core.http.WebSocketConnectOptions;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.walrex.application.port.input.FinnhubTradeUseCase;
import org.walrex.application.port.output.RemittanceRouteOutputPort;
import org.walrex.domain.model.MarketPairAction;
import org.walrex.domain.model.MarketPairChangedEvent;
import org.walrex.domain.model.SubscriptionState;
import org.walrex.infrastructure.adapter.inbound.websocket.dto.FinnhubMessage;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@ApplicationScoped
public class FinnhubStreamManager {

    private static final String WS_HOST = "ws.finnhub.io";
    private static final int WS_PORT = 443;
    private static final long HEARTBEAT_CHECK_MS = 30_000L;
    private static final long HEARTBEAT_TIMEOUT_MS = 90_000L;

    @ConfigProperty(name = "finnhub.api-token")
    String apiToken;

    @ConfigProperty(name = "finnhub.symbol-provider", defaultValue = "OANDA")
    String symbolProvider;

    @ConfigProperty(name = "finnhub.reconnect-delay-ms", defaultValue = "5000")
    long reconnectDelayMs;

    @Inject
    Vertx vertx;

    @Inject
    FinnhubTradeUseCase tradeUseCase;

    @Inject
    RemittanceRouteOutputPort routeOutputPort;

    @Inject
    ObjectMapper objectMapper;

    private final ConcurrentHashMap<String, SubscriptionState> subscriptions = new ConcurrentHashMap<>();
    private final AtomicBoolean shutdownRequested = new AtomicBoolean(false);

    private volatile HttpClient httpClient;
    private volatile WebSocket currentWs;
    private volatile Instant lastHeartbeat = Instant.now();
    private volatile long watchdogTimerId = -1;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    void onStart(@Observes StartupEvent event) {
        vertx.setTimer(2000, id -> {
            // Hibernate Reactive Panache requiere un duplicated context seguro.
            // vertx.setTimer() corre en el contexto raíz → duplicamos explícitamente.
            Context safeCtx = ((ContextInternal) vertx.getOrCreateContext()).duplicate();
            VertxContextSafetyToggle.setContextSafe(safeCtx, true);
            safeCtx.runOnContext(v -> initFromDatabase());
        });
    }

    void onStop(@Observes ShutdownEvent event) {
        shutdownRequested.set(true);
        if (watchdogTimerId != -1) {
            vertx.cancelTimer(watchdogTimerId);
        }
        if (httpClient != null) {
            httpClient.close();
        }
    }

    // ── CDI event: suscripción dinámica ───────────────────────────────────────

    public void onMarketPairChanged(@Observes MarketPairChangedEvent event) {
        String symbol = event.getSymbol();

        if (event.getAction() == MarketPairAction.ADD) {
            if (!subscriptions.containsKey(symbol)) {
                subscriptions.put(symbol, SubscriptionState.builder()
                        .symbol(symbol)
                        .active(false)
                        .subscribedAt(Instant.now())
                        .build());
                log.info("[FinnhubStreamManager] Par añadido dinámicamente: {}", symbol);
                vertx.runOnContext(v -> sendSubscribe(symbol));
            } else {
                log.debug("[FinnhubStreamManager] Par ya suscrito, ignorando ADD: {}", symbol);
            }
        } else {
            subscriptions.remove(symbol);
            log.info("[FinnhubStreamManager] Par removido dinámicamente: {}", symbol);
            vertx.runOnContext(v -> sendUnsubscribe(symbol));
        }
    }

    // ── Inicialización desde BD ───────────────────────────────────────────────

    private void initFromDatabase() {
        routeOutputPort.findActiveRoutesByProvider("ASTROPAY")
                .subscribe().with(
                        routes -> {
                            if (routes.isEmpty()) {
                                log.warn("[FinnhubStreamManager] No hay rutas ASTROPAY activas — sin conexión");
                                return;
                            }
                            // Solo suscribir rutas donde code_quote != PEN.
                            // Se mapea la moneda QUOTE al par USD de Finnhub (OANDA:USD_COP, OANDA:EUR_USD…).
                            // EUR→PEN no se suscribe: su tasa AstroPay se actualiza cuando cambia OANDA:EUR_USD.
                            routes.stream()
                                    .filter(r -> !"PEN".equalsIgnoreCase(r.getCurrencyToCode()))
                                    .map(r -> currencyToFinnhubSymbol(symbolProvider, r.getCurrencyToCode()))
                                    .filter(Objects::nonNull)
                                    .distinct()
                                    .forEach(symbol -> subscriptions.putIfAbsent(symbol,
                                            SubscriptionState.builder()
                                                    .symbol(symbol)
                                                    .active(false)
                                                    .subscribedAt(Instant.now())
                                                    .build()));
                            log.info("[FinnhubStreamManager] Símbolos iniciales cargados: {}", subscriptions.keySet());
                            connect();
                        },
                        e -> log.error("[FinnhubStreamManager] Error cargando rutas ASTROPAY: {}", e.getMessage(), e)
                );
    }

    /**
     * Maps a single currency code to its primary Finnhub OANDA symbol.
     * Major currencies (EUR, GBP, AUD, NZD, CHF) are quoted as base against USD.
     * Emerging-market currencies (PEN, BRL, COP, etc.) use USD as the base.
     * Returns null for USD (no self-pair needed).
     */
    public static String currencyToFinnhubSymbol(String oandaProvider, String code) {
        return switch (code.toUpperCase()) {
            case "EUR", "GBP", "AUD", "NZD", "CHF" -> oandaProvider + ":" + code + "_USD";
            case "BRL", "COP" -> "BINANCE:USDT" + code.toUpperCase();
            default -> null;
        };
    }

    // ── Conexión WebSocket ────────────────────────────────────────────────────

    private void connect() {
        if (subscriptions.isEmpty()) {
            log.warn("[FinnhubStreamManager] Sin símbolos registrados, no se conecta");
            return;
        }

        httpClient = vertx.createHttpClient(new HttpClientOptions()
                .setSsl(true)
                .setDefaultHost(WS_HOST)
                .setDefaultPort(WS_PORT));

        httpClient.webSocket(new WebSocketConnectOptions()
                        .setHost(WS_HOST)
                        .setPort(WS_PORT)
                        .setURI("/?token=" + apiToken)
                        .setSsl(true))
                .onSuccess(this::onConnected)
                .onFailure(e -> {
                    log.error("[FinnhubStreamManager] Conexión fallida: {} — reintento en {}ms",
                            e.getMessage(), reconnectDelayMs);
                    scheduleReconnect();
                });
    }

    private void onConnected(WebSocket ws) {
        log.info("[FinnhubStreamManager] WebSocket conectado — {} símbolos en memoria", subscriptions.size());
        currentWs = ws;
        lastHeartbeat = Instant.now();

        ws.textMessageHandler(this::handleRawMessage);
        ws.closeHandler(v -> {
            log.warn("[FinnhubStreamManager] Conexión cerrada — reintento en {}ms", reconnectDelayMs);
            currentWs = null;
            markAllInactive();
            scheduleReconnect();
        });
        ws.exceptionHandler(e ->
                log.error("[FinnhubStreamManager] Error en WebSocket: {}", e.getMessage(), e));

        // Re-suscribir todos los símbolos registrados en memoria
        subscriptions.keySet().forEach(this::sendSubscribe);

        // Arrancar watchdog solo una vez (persiste entre reconexiones)
        if (watchdogTimerId == -1) {
            watchdogTimerId = vertx.setPeriodic(HEARTBEAT_CHECK_MS, id -> checkHeartbeat());
            log.debug("[FinnhubStreamManager] Watchdog iniciado (cada {}ms)", HEARTBEAT_CHECK_MS);
        }
    }

    // ── Suscripción dinámica ──────────────────────────────────────────────────

    private void sendSubscribe(String symbol) {
        WebSocket ws = currentWs;
        if (ws == null || ws.isClosed()) {
            log.debug("[FinnhubStreamManager] WS no disponible — suscripción a {} pendiente hasta reconexión", symbol);
            return;
        }
        String payload = "{\"type\":\"subscribe\",\"symbol\":\"" + symbol + "\"}";
        ws.writeTextMessage(payload, ar -> {
            if (ar.succeeded()) {
                SubscriptionState state = subscriptions.get(symbol);
                if (state != null) state.setActive(true);
                log.debug("[FinnhubStreamManager] Suscrito a {}", symbol);
            } else {
                log.error("[FinnhubStreamManager] Error suscribiendo {}: {}", symbol, ar.cause().getMessage());
            }
        });
    }

    private void sendUnsubscribe(String symbol) {
        WebSocket ws = currentWs;
        if (ws == null || ws.isClosed()) return;
        String payload = "{\"type\":\"unsubscribe\",\"symbol\":\"" + symbol + "\"}";
        ws.writeTextMessage(payload, ar -> {
            if (ar.succeeded()) {
                log.debug("[FinnhubStreamManager] Desuscrito de {}", symbol);
            } else {
                log.error("[FinnhubStreamManager] Error desuscribiendo {}: {}", symbol, ar.cause().getMessage());
            }
        });
    }

    // ── Procesamiento de mensajes ─────────────────────────────────────────────

    private void handleRawMessage(String raw) {
        lastHeartbeat = Instant.now();
        try {
            FinnhubMessage msg = objectMapper.readValue(raw, FinnhubMessage.class);

            switch (msg.getType() != null ? msg.getType() : "") {
                case "trade" -> {
                    if (msg.getData() != null) {
                        msg.getData().forEach(trade -> {
                            if (trade.getPrice() == null || trade.getSymbol() == null) return;

                            Instant time = trade.getTimestamp() != null
                                    ? Instant.ofEpochMilli(trade.getTimestamp())
                                    : Instant.now();

                            SubscriptionState state = subscriptions.get(trade.getSymbol());
                            if (state != null) {
                                state.setLastPrice(trade.getPrice());
                                state.setLastUpdated(time);
                            }

                            tradeUseCase.onTrade(trade.getSymbol(), trade.getPrice(), time)
                                    .subscribe().with(
                                            __ -> {},
                                            e -> log.error("[FinnhubStreamManager] Error procesando trade {}: {}",
                                                    trade.getSymbol(), e.getMessage())
                                    );
                        });
                    }
                }
                case "ping" -> log.trace("[FinnhubStreamManager] ping recibido");
                case "error" -> log.error("[FinnhubStreamManager] Error del servidor: {}", msg.getMsg());
                default -> log.debug("[FinnhubStreamManager] Tipo desconocido={}", msg.getType());
            }
        } catch (Exception e) {
            log.error("[FinnhubStreamManager] Error parseando mensaje: {}", raw, e);
        }
    }

    // ── Watchdog ──────────────────────────────────────────────────────────────

    private void checkHeartbeat() {
        if (shutdownRequested.get()) return;

        long elapsedMs = Instant.now().toEpochMilli() - lastHeartbeat.toEpochMilli();
        if (elapsedMs > HEARTBEAT_TIMEOUT_MS) {
            log.warn("[FinnhubStreamManager] Sin actividad en {}ms — forzando reconexión", elapsedMs);
            WebSocket ws = currentWs;
            if (ws != null && !ws.isClosed()) {
                ws.close(); // cierra → closeHandler dispara scheduleReconnect()
            } else {
                markAllInactive();
                scheduleReconnect();
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void scheduleReconnect() {
        if (shutdownRequested.get()) return;
        vertx.setTimer(reconnectDelayMs, id -> connect());
    }

    private void markAllInactive() {
        subscriptions.values().forEach(s -> s.setActive(false));
    }

    public Set<String> getSubscribedSymbols() {
        return subscriptions.keySet();
    }
}
