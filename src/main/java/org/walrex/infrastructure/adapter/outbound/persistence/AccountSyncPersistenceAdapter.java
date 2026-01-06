package org.walrex.infrastructure.adapter.outbound.persistence;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.walrex.application.port.output.AccountSyncQueryPort;
import org.walrex.domain.model.AccountingAccount;
import org.walrex.infrastructure.adapter.logging.LogExecutionTime;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.AccountingAccountEntity;
import org.walrex.infrastructure.adapter.outbound.persistence.mapper.AccountingAccountMapper;

/**
 * Adapter de persistencia para operaciones de sincronización de cuentas contables.
 * Implementa consultas específicas para el proceso de embeddings.
 */
@Slf4j
@ApplicationScoped
public class AccountSyncPersistenceAdapter implements AccountSyncQueryPort {

    @Inject
    AccountingAccountMapper mapper;

    @Override
    @WithSpan("AccountSyncPersistenceAdapter.findUnsyncedAccounts")
    @LogExecutionTime(value = LogExecutionTime.LogLevel.DEBUG)
    public Multi<AccountingAccount> findUnsyncedAccounts() {
        log.debug("Finding unsynced accounts");

        return AccountingAccountEntity
                .<AccountingAccountEntity>find(
                        "embeddingsSynced = false AND active = true AND deletedAt IS NULL"
                )
                .list()
                .onItem().transformToMulti(list -> Multi.createFrom().iterable(list))
                .onItem().transform(entity -> {
                    log.trace("Found unsynced account: {} ({})", entity.getCode(), entity.getName());
                    return mapper.toDomain(entity);
                });
    }

    @Override
    @WithSpan("AccountSyncPersistenceAdapter.findById")
    @LogExecutionTime(value = LogExecutionTime.LogLevel.DEBUG, logParameters = true)
    public Uni<AccountingAccount> findById(Integer accountId) {
        log.debug("Finding account by ID: {}", accountId);

        return AccountingAccountEntity.<AccountingAccountEntity>findById(accountId)
                .onItem().transform(entity -> {
                    if (entity == null) {
                        log.warn("Account not found: {}", accountId);
                        return null;
                    }
                    return mapper.toDomain(entity);
                });
    }

    @Override
    @WithSpan("AccountSyncPersistenceAdapter.markAsSynced")
    @LogExecutionTime(value = LogExecutionTime.LogLevel.DEBUG, logParameters = true)
    public Uni<Void> markAsSynced(Integer accountId) {
        log.debug("Marking account {} as synced", accountId);

        return Panache.withTransaction(() ->
                AccountingAccountEntity.<AccountingAccountEntity>findById(accountId)
                        .onItem().transformToUni(entity -> {
                            if (entity == null) {
                                log.warn("Cannot mark as synced - account not found: {}", accountId);
                                return Uni.createFrom().voidItem();
                            }

                            entity.setEmbeddingsSynced(true);
                            entity.setUpdatedAt(java.time.OffsetDateTime.now());

                            return entity.persistAndFlush()
                                    .onItem().invoke(() ->
                                            log.debug("Account {} marked as synced", accountId)
                                    )
                                    .replaceWithVoid();
                        })
        );
    }

    @Override
    @WithSpan("AccountSyncPersistenceAdapter.markAsUnsynced")
    @LogExecutionTime(value = LogExecutionTime.LogLevel.DEBUG, logParameters = true)
    public Uni<Void> markAsUnsynced(Integer accountId) {
        log.debug("Marking account {} as unsynced", accountId);

        return Panache.withTransaction(() ->
                AccountingAccountEntity.<AccountingAccountEntity>findById(accountId)
                        .onItem().transformToUni(entity -> {
                            if (entity == null) {
                                log.warn("Cannot mark as unsynced - account not found: {}", accountId);
                                return Uni.createFrom().voidItem();
                            }

                            entity.setEmbeddingsSynced(false);
                            entity.setUpdatedAt(java.time.OffsetDateTime.now());

                            return entity.persistAndFlush()
                                    .onItem().invoke(() ->
                                            log.debug("Account {} marked as unsynced", accountId)
                                    )
                                    .replaceWithVoid();
                        })
        );
    }

    @Override
    @WithSpan("AccountSyncPersistenceAdapter.countUnsyncedAccounts")
    @LogExecutionTime(value = LogExecutionTime.LogLevel.DEBUG)
    public Uni<Long> countUnsyncedAccounts() {
        log.debug("Counting unsynced accounts");

        return AccountingAccountEntity.count(
                "embeddingsSynced = false AND active = true AND deletedAt IS NULL"
        ).onItem().invoke(count ->
                log.debug("Found {} unsynced accounts", count)
        );
    }

    @Override
    @WithSpan("AccountSyncPersistenceAdapter.markAllAsUnsynced")
    @LogExecutionTime(value = LogExecutionTime.LogLevel.INFO)
    public Uni<Void> markAllAsUnsynced() {
        log.info("Marking all active accounts as unsynced");

        return Panache.withTransaction(() ->
                AccountingAccountEntity.update(
                        "embeddingsSynced = false, updatedAt = ?1 WHERE active = true AND deletedAt IS NULL",
                        java.time.OffsetDateTime.now()
                ).onItem().invoke(count ->
                        log.info("Marked {} accounts as unsynced", count)
                ).replaceWithVoid()
        );
    }
}
