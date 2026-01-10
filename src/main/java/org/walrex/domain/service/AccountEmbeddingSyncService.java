package org.walrex.domain.service;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.walrex.application.port.input.SyncAccountEmbeddingsUseCase;
import org.walrex.application.port.output.AccountSyncQueryPort;
import org.walrex.application.port.output.VectorStorePort;
import org.walrex.domain.exception.VectorDimensionMismatchException;
import org.walrex.domain.model.AccountChunk;
import org.walrex.domain.model.AccountingAccount;
import org.walrex.domain.model.SyncResult;
import org.walrex.infrastructure.adapter.logging.LogExecutionTime;

import java.time.OffsetDateTime;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Servicio de dominio que implementa la sincronización de embeddings de cuentas contables.
 * Orquesta el flujo completo: chunking, generación de embeddings, almacenamiento en Qdrant.
 */
@Slf4j
@ApplicationScoped
public class AccountEmbeddingSyncService implements SyncAccountEmbeddingsUseCase {

    @Inject
    AccountChunkingService chunkingService;

    @Inject
    AccountSyncQueryPort accountSyncQueryPort;

    @Inject
    VectorStorePort vectorStorePort;

    @Inject
    Vertx vertx;

    @ConfigProperty(name = "embeddings.sync.batch-size", defaultValue = "50")
    Integer batchSize;

    @Override
    @WithSpan("AccountEmbeddingSyncService.syncUnsyncedAccounts")
    @LogExecutionTime(value = LogExecutionTime.LogLevel.INFO)
    @io.quarkus.hibernate.reactive.panache.common.WithTransaction
    public Uni<SyncResult> syncUnsyncedAccounts() {
        log.info("Starting synchronization of unsynced accounts (batch size: {})", batchSize);

        OffsetDateTime startedAt = OffsetDateTime.now();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        AtomicInteger totalCount = new AtomicInteger(0);

        SyncResult result = SyncResult.builder()
                .startedAt(startedAt)
                .build();

        return accountSyncQueryPort.findUnsyncedAccounts()
                .onItem().transformToUniAndConcatenate(account -> {
                    totalCount.incrementAndGet();
                    return syncSingleAccount(account)
                            .onItem().invoke(() -> {
                                successCount.incrementAndGet();
                                log.debug("Successfully synced account: {} ({})",
                                        account.getCode(), account.getName());
                            })
                            .onFailure().recoverWithItem(throwable -> {
                                // FAIL-FAST: Si es error crítico de dimensiones, propagar inmediatamente
                                if (throwable instanceof VectorDimensionMismatchException) {
                                    log.error("🚨 CRITICAL CONFIGURATION ERROR DETECTED! " +
                                             "Stopping all sync operations to prevent API consumption. " +
                                             "Error: {}", throwable.getMessage());
                                    throw (VectorDimensionMismatchException) throwable;
                                }

                                // Para otros errores, continuar con el siguiente account
                                failCount.incrementAndGet();
                                log.error("Failed to sync account: {} ({})",
                                        account.getCode(), account.getName(), throwable);
                                result.addError(account.getId(), throwable.getMessage());
                                return null;
                            });
                })
                .collect().asList()
                .onItem().transform(ignored -> {
                    OffsetDateTime completedAt = OffsetDateTime.now();
                    long durationMs = java.time.Duration.between(startedAt, completedAt).toMillis();

                    result.setTotalProcessed(totalCount.get());
                    result.setSuccessfulSyncs(successCount.get());
                    result.setFailedSyncs(failCount.get());
                    result.setSkippedAccounts(0);
                    result.setCompletedAt(completedAt);
                    result.setDurationMs(durationMs);
                    result.setSuccessful(failCount.get() == 0);

                    log.info("Synchronization completed. Total: {}, Success: {}, Failed: {}, Duration: {}ms",
                            result.getTotalProcessed(),
                            result.getSuccessfulSyncs(),
                            result.getFailedSyncs(),
                            result.getDurationMs());

                    return result;
                });
    }

    @Override
    @WithSpan("AccountEmbeddingSyncService.syncAccount")
    @LogExecutionTime(value = LogExecutionTime.LogLevel.INFO, logParameters = true)
    public Uni<Void> syncAccount(Integer accountId) {
        log.info("Syncing specific account ID: {}", accountId);

        return accountSyncQueryPort.findById(accountId)
                .onItem().transformToUni(account -> {
                    if (account == null) {
                        log.warn("Account not found: {}", accountId);
                        return Uni.createFrom().failure(
                                new IllegalArgumentException("Account not found: " + accountId)
                        );
                    }
                    return syncSingleAccount(account);
                });
    }

    @Override
    @WithSpan("AccountEmbeddingSyncService.removeSyncedAccount")
    @LogExecutionTime(value = LogExecutionTime.LogLevel.INFO, logParameters = true)
    public Uni<Void> removeSyncedAccount(Integer accountId) {
        log.info("Removing synced account ID: {} from vector store", accountId);

        return vectorStorePort.deleteAccountEmbedding(accountId)
                .call(() -> accountSyncQueryPort.markAsUnsynced(accountId))
                .onItem().invoke(() ->
                        log.info("Successfully removed account {} from vector store", accountId)
                )
                .onFailure().invoke(throwable ->
                        log.error("Failed to remove account {} from vector store", accountId, throwable)
                );
    }

    @Override
    @WithSpan("AccountEmbeddingSyncService.forceResyncAll")
    @LogExecutionTime(value = LogExecutionTime.LogLevel.INFO)
    public Uni<SyncResult> forceResyncAll() {
        log.info("Forcing resynchronization of all active accounts");

        return accountSyncQueryPort.markAllAsUnsynced()
                .call(() -> {
                    log.info("All accounts marked as unsynced. Starting synchronization...");
                    return Uni.createFrom().voidItem();
                })
                .chain(() -> syncUnsyncedAccounts());
    }

    /**
     * Sincroniza una sola cuenta: genera chunk, embedding y almacena en Qdrant.
     */
    private Uni<Void> syncSingleAccount(AccountingAccount account) {
        log.debug("Processing account: {} - {}", account.getCode(), account.getName());

        return chunkingService.createAccountChunk(account)
                .chain(chunk -> vectorStorePort.upsertAccountEmbedding(chunk))
                // CRÍTICO: Volver al event-loop de Vert.x antes de operación DB con Hibernate Reactive
                .emitOn(command -> vertx.getOrCreateContext().runOnContext(v -> command.run()))
                .chain(() -> accountSyncQueryPort.markAsSynced(account.getId()))
                .onFailure().invoke(throwable ->
                        log.error("Error syncing account {}: {}", account.getCode(), throwable.getMessage())
                );
    }
}
