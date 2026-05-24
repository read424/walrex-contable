package org.walrex.infrastructure.adapter.inbound.scheduler;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.ScheduledExecution;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.walrex.application.port.input.UpdateAstroPayExchangeRateUseCase;
import org.walrex.application.port.input.UpdateExchangeRatesUseCase;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Scheduler de polling Binance P2P para tasas de cambio.
 *
 * DESHABILITADO: reemplazado por FinnhubWebSocketAdapter (WebSocket en tiempo real).
 * Para re-activar, establecer en application.yml:
 *   walrex.scheduler.exchange-rate.cron=0 *\/10 * * * ?
 *   walrex.scheduler.exchange-rate.initial.every=10s
 */
@Slf4j
@ApplicationScoped
public class ExchangeRateScheduler {

    @Inject
    UpdateExchangeRatesUseCase exchangeRateService;

    @Inject
    UpdateAstroPayExchangeRateUseCase astroPayService;

    /**
     * Predicado para deshabilitar el scheduler inicial después de la primera ejecución
     */
    public static class SkipAfterFirstExecution implements Scheduled.SkipPredicate {
        private static final AtomicBoolean hasExecuted = new AtomicBoolean(false);

        @Override
        public boolean test(ScheduledExecution execution) {
            return shouldSkipExecution();
        }

        /**
         * Determina si se debe saltar la ejecución del scheduler
         *
         * @return true si ya se ejecutó una vez (debe saltar), false si es la primera vez (debe ejecutar)
         */
        private boolean shouldSkipExecution() {
            // Primera llamada: hasExecuted = false, retorna false (NO skip, ejecuta), luego se setea a true
            // Llamadas subsiguientes: hasExecuted = true, retorna true (SÍ skip, no ejecuta)
            return hasExecuted.getAndSet(true);
        }
    }

    @Scheduled(cron = "${walrex.scheduler.exchange-rate.cron:off}",
               concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    public Uni<Void> updateExchangeRatesScheduled() {
        log.info("=== [SCHEDULER] Triggered at {} ===", LocalDateTime.now());
        return performUpdate("[SCHEDULER]");
    }

    @Scheduled(every = "${walrex.scheduler.exchange-rate.initial.every:off}",
               delay = 10, delayUnit = java.util.concurrent.TimeUnit.SECONDS,
               skipExecutionIf = SkipAfterFirstExecution.class,
               concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    public Uni<Void> updateExchangeRatesInitial() {
        log.info("=== [INITIAL UPDATE] Triggered at {} ===", LocalDateTime.now());
        return performUpdate("[INITIAL UPDATE]");
    }

    /**
     * Método común que ejecuta la actualización de tasas de cambio
     * Llamado tanto por el scheduler periódico como por el inicial
     *
     * @param context Contexto de ejecución para logging ("[SCHEDULER]" o "[INITIAL UPDATE]")
     * @return Uni<Void> para que Quarkus Scheduler se suscriba automáticamente
     */
    private Uni<Void> performUpdate(String context) {
        return exchangeRateService.updateExchangeRates()
                .invoke(update -> {
                    log.info("=== {} Exchange rates updated successfully ===", context);
                    log.info("=== {} Processed {} currency pairs ===", context, update.ratesByPair().size());
                })
                .onFailure().invoke(failure ->
                    log.error("=== {} Failed to update exchange rates ===", context, failure)
                )
                .replaceWithVoid()
                .chain(() -> astroPayService.updateRatesForActiveRoutes()
                        .invoke(() -> log.info("=== {} AstroPay rates updated ===", context))
                        .onFailure().invoke(e ->
                                log.error("=== {} Failed to update AstroPay rates: {} ===", context, e.getMessage()))
                        .onFailure().recoverWithNull());
    }
}
