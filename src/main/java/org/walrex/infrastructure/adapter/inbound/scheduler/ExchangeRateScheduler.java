package org.walrex.infrastructure.adapter.inbound.scheduler;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.ScheduledExecution;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.walrex.domain.service.ExchangeRateService;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Scheduler para actualizar tasas de cambio cada 3 minutos
 *
 * ARQUITECTURA:
 * - updateExchangeRatesScheduled() se ejecuta cada 3 minutos (cron)
 * - updateExchangeRatesInitial() se ejecuta una sola vez, 10 segundos después del inicio
 * - Usa Scheduled.SkipPredicate para deshabilitar el scheduler inicial después de la primera ejecución
 * - Ambos usan @Scheduled que proporciona el contexto duplicado de Vert.x requerido
 * - ExchangeRateService contiene la lógica reactiva con Mutiny
 * - @WithSession se maneja en los adaptadores de persistencia
 */
@Slf4j
@ApplicationScoped
public class ExchangeRateScheduler {

    @Inject
    ExchangeRateService exchangeRateService;

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

    /**
     * Ejecuta la actualización de tasas cada 10 minutos
     *
     * IMPORTANTE:
     * - @Scheduled proporciona automáticamente el contexto duplicado de Vert.x
     * - Llama al método común performUpdate()
     */
    @Scheduled(cron = "0 */10 * * * ?", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    public Uni<Void> updateExchangeRatesScheduled() {
        log.info("=== [SCHEDULER] Triggered at {} ===", LocalDateTime.now());
        return performUpdate("[SCHEDULER]");
    }

    /**
     * Ejecuta la primera actualización de tasas 10 segundos después del inicio
     *
     * IMPORTANTE:
     * - Se ejecuta cada 10 segundos PERO skipExecutionIf lo deshabilita después de la primera vez
     * - SkipAfterFirstExecution retorna false la primera vez (ejecuta) y true después (no ejecuta)
     * - @Scheduled proporciona el contexto duplicado de Vert.x que requiere Hibernate Reactive
     * - Llama al método común performUpdate()
     */
    @Scheduled(every = "10s", delay = 10, delayUnit = java.util.concurrent.TimeUnit.SECONDS,
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
                .replaceWithVoid();
    }
}
