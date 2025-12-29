package org.walrex.infrastructure.adapter.inbound.scheduler;

//import io.quarkus.runtime.StartupEvent;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.Scheduled;
//import io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle;
//import io.vertx.core.Context;
//import io.vertx.core.Handler;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;
import jakarta.enterprise.context.ApplicationScoped;
//import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.walrex.domain.service.ExchangeRateService;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Scheduler para actualizar tasas de cambio cada 2 minutos (TEST)
 *
 * ARQUITECTURA:
 * - Scheduler (@Scheduled) = Trigger imperativo en worker thread
 * - vertx.runOnContext() = Ejecuta en EventLoop thread
 * - ExchangeRateService = Lógica reactiva con Hibernate Reactive
 *
 * Esta separación es CRÍTICA para que Hibernate Reactive funcione correctamente
 */
@Slf4j
@ApplicationScoped
public class ExchangeRateScheduler {

    @Inject
    ExchangeRateService exchangeRateService;

    /**
     * Ejecuta la actualización de tasas cada 10 minutos
     * Cron: cada 10 minutos
     *
     * IMPORTANTE: Scheduler solo actúa como TRIGGER
     * La lógica reactiva se ejecuta dentro del EventLoop vía vertx.runOnContext()
     */
    @Scheduled(cron = "0 */2 * * * ?", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    public Uni<Void> updateExchangeRates() {
        log.info("=== Scheduler triggered at {} ===", java.time.LocalDateTime.now());

        return exchangeRateService.updateExchangeRates()
                .invoke(update->{
                    log.info("=== Exchange rates updated successfully ===");
                    log.info("Processed {} currency pairs ", update.ratesByPair());
                })
                .onFailure()
                .invoke(failure->
                    log.error("=== Failed to update exchange rates ===", failure)
                )
                .replaceWithVoid();
    }

    /**
     * Ejecuta al inicio de la aplicación para tener tasas inmediatamente
     * Se ejecuta 10 segundos después del arranque para permitir que los beans estén listos
     */
    public Uni<Void> onStart(@Observes StartupEvent event) {
        log.info("=== Application started - Initial exchange rate update in 10 seconds ===");

        // Programar primera actualización 10 segundos después del inicio
        return Uni.createFrom().voidItem()
                .onItem().delayIt().by(Duration.ofSeconds(10))
                .onItem().transformToUni(v->{
                    log.info("=== Running initial exchange rate update ====");
                    return updateExchangeRates();
                });
    }
}
