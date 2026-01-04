package org.walrex.infrastructure.adapter.outbound.listener;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.hibernate.reactive.mutiny.Mutiny;
import org.walrex.application.port.input.SyncAccountEmbeddingsUseCase;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.AccountingAccountEntity;

/**
 * Event listener para sincronización automática de embeddings cuando se crean o actualizan cuentas.
 * Se ejecuta de forma asíncrona para no bloquear la operación principal.
 */
@Slf4j
@ApplicationScoped
public class AccountSyncEventListener {

    @Inject
    SyncAccountEmbeddingsUseCase syncAccountEmbeddingsUseCase;

    @ConfigProperty(name = "embeddings.sync.enabled", defaultValue = "true")
    Boolean syncEnabled;

    /**
     * Listener para eventos de persistencia (insert).
     * Se ejecuta después de que una cuenta es creada.
     */
    public void onAccountCreated(AccountingAccountEntity account) {
        if (!syncEnabled) {
            log.debug("Embeddings sync is disabled, skipping account {}", account.getId());
            return;
        }

        if (!Boolean.TRUE.equals(account.getActive())) {
            log.debug("Account {} is not active, skipping sync", account.getId());
            return;
        }

        log.info("Account created: {} ({}). Triggering async embedding sync",
                account.getId(), account.getCode());

        // Ejecutar sincronización de forma asíncrona (fire and forget)
        syncAccountEmbeddingsUseCase.syncAccount(account.getId())
                .subscribe().with(
                        success -> log.info("Successfully synced new account: {} ({})",
                                account.getId(), account.getCode()),
                        failure -> log.error("Failed to sync new account: {} ({})",
                                account.getId(), account.getCode(), failure)
                );
    }

    /**
     * Listener para eventos de actualización.
     * Se ejecuta después de que una cuenta es actualizada.
     */
    public void onAccountUpdated(AccountingAccountEntity account) {
        if (!syncEnabled) {
            log.debug("Embeddings sync is disabled, skipping account {}", account.getId());
            return;
        }

        // Si la cuenta fue desactivada, remover del vector store
        if (!Boolean.TRUE.equals(account.getActive())) {
            log.info("Account {} was deactivated. Removing from vector store", account.getId());

            syncAccountEmbeddingsUseCase.removeSyncedAccount(account.getId())
                    .subscribe().with(
                            success -> log.info("Successfully removed account {} from vector store",
                                    account.getId()),
                            failure -> log.error("Failed to remove account {} from vector store",
                                    account.getId(), failure)
                    );
            return;
        }

        log.info("Account updated: {} ({}). Triggering async embedding resync",
                account.getId(), account.getCode());

        // Resincronizar (upsert en Qdrant)
        syncAccountEmbeddingsUseCase.syncAccount(account.getId())
                .subscribe().with(
                        success -> log.info("Successfully resynced updated account: {} ({})",
                                account.getId(), account.getCode()),
                        failure -> log.error("Failed to resync updated account: {} ({})",
                                account.getId(), account.getCode(), failure)
                );
    }

    /**
     * Listener para eventos de eliminación.
     * Se ejecuta antes de que una cuenta sea eliminada.
     */
    public void onAccountDeleted(AccountingAccountEntity account) {
        if (!syncEnabled) {
            log.debug("Embeddings sync is disabled, skipping account {}", account.getId());
            return;
        }

        log.info("Account deleted: {} ({}). Removing from vector store",
                account.getId(), account.getCode());

        syncAccountEmbeddingsUseCase.removeSyncedAccount(account.getId())
                .subscribe().with(
                        success -> log.info("Successfully removed deleted account {} from vector store",
                                account.getId()),
                        failure -> log.error("Failed to remove deleted account {} from vector store",
                                account.getId(), failure)
                );
    }
}
