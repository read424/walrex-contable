package org.walrex.infrastructure.adapter.inbound.scheduler;

import io.quarkus.scheduler.Scheduled;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.walrex.application.port.output.RemittanceRouteOutputPort;
import org.walrex.domain.model.MarketPairAction;
import org.walrex.domain.model.MarketPairChangedEvent;
import org.walrex.infrastructure.adapter.inbound.websocket.FinnhubStreamManager;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Reconcilia cada 24 h las rutas ASTROPAY activas en BD
 * contra los símbolos suscritos en FinnhubStreamManager.
 *
 * Emite MarketPairChangedEvent(ADD) para símbolos nuevos en BD
 * y MarketPairChangedEvent(REMOVE) para los que ya no están en BD.
 */
@Slf4j
@ApplicationScoped
public class MarketSyncReconciliationScheduler {

    @ConfigProperty(name = "finnhub.symbol-provider", defaultValue = "OANDA")
    String symbolProvider;

    @Inject
    RemittanceRouteOutputPort routeOutputPort;

    @Inject
    FinnhubStreamManager streamManager;

    @Inject
    Event<MarketPairChangedEvent> marketPairEvent;

    @Scheduled(every = "${walrex.scheduler.market-sync.every:24h}",
               concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    public Uni<Void> reconcile() {
        log.info("[MarketSyncReconciliation] Iniciando reconciliación de pares de mercado");

        return routeOutputPort.findActiveRoutesByProvider("ASTROPAY")
                .invoke(routes -> {
                    Set<String> dbSymbols = routes.stream()
                            .filter(r -> !"PEN".equalsIgnoreCase(r.getCurrencyToCode()))
                            .map(r -> FinnhubStreamManager.currencyToFinnhubSymbol(symbolProvider, r.getCurrencyToCode()))
                            .filter(Objects::nonNull)
                            .collect(Collectors.toSet());

                    Set<String> inMemorySymbols = streamManager.getSubscribedSymbols();

                    Set<String> toAdd = dbSymbols.stream()
                            .filter(s -> !inMemorySymbols.contains(s))
                            .collect(Collectors.toSet());

                    Set<String> toRemove = inMemorySymbols.stream()
                            .filter(s -> !dbSymbols.contains(s))
                            .collect(Collectors.toSet());

                    if (toAdd.isEmpty() && toRemove.isEmpty()) {
                        log.info("[MarketSyncReconciliation] Sin cambios — {} pares en sync", inMemorySymbols.size());
                        return;
                    }

                    log.info("[MarketSyncReconciliation] Añadir={} Remover={}", toAdd, toRemove);

                    toAdd.forEach(symbol ->
                            marketPairEvent.fire(new MarketPairChangedEvent(symbol, MarketPairAction.ADD)));

                    toRemove.forEach(symbol ->
                            marketPairEvent.fire(new MarketPairChangedEvent(symbol, MarketPairAction.REMOVE)));

                    log.info("[MarketSyncReconciliation] Reconciliación completada — +{} -{} pares",
                            toAdd.size(), toRemove.size());
                })
                .replaceWithVoid()
                .onFailure().invoke(e ->
                        log.error("[MarketSyncReconciliation] Error consultando rutas ASTROPAY: {}", e.getMessage(), e));
    }
}
