package org.walrex.infrastructure.adapter.outbound.listener;

import jakarta.enterprise.inject.spi.CDI;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PreRemove;
import lombok.extern.slf4j.Slf4j;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.AccountingAccountEntity;

/**
 * JPA Entity Listener para AccountingAccountEntity.
 * Se ejecuta en respuesta a eventos del ciclo de vida de la entidad.
 *
 * Compatible con Hibernate Reactive - usa CDI para obtener beans.
 */
@Slf4j
public class AccountEntityListener {

    /**
     * Se ejecuta después de que una nueva cuenta es persistida en la DB.
     * Trigger la sincronización de embeddings de forma asíncrona.
     */
    @PostPersist
    public void onPostPersist(AccountingAccountEntity account) {
        log.debug("PostPersist callback triggered for account: {}", account.getId());

        try {
            AccountSyncEventListener listener = CDI.current()
                    .select(AccountSyncEventListener.class)
                    .get();

            listener.onAccountCreated(account);
        } catch (Exception e) {
            log.error("Error in PostPersist callback for account {}", account.getId(), e);
        }
    }

    /**
     * Se ejecuta después de que una cuenta es actualizada en la DB.
     * Trigger la resincronización de embeddings de forma asíncrona.
     */
    @PostUpdate
    public void onPostUpdate(AccountingAccountEntity account) {
        log.debug("PostUpdate callback triggered for account: {}", account.getId());

        try {
            AccountSyncEventListener listener = CDI.current()
                    .select(AccountSyncEventListener.class)
                    .get();

            listener.onAccountUpdated(account);
        } catch (Exception e) {
            log.error("Error in PostUpdate callback for account {}", account.getId(), e);
        }
    }

    /**
     * Se ejecuta antes de que una cuenta sea eliminada de la DB.
     * Remueve los embeddings del vector store.
     */
    @PreRemove
    public void onPreRemove(AccountingAccountEntity account) {
        log.debug("PreRemove callback triggered for account: {}", account.getId());

        try {
            AccountSyncEventListener listener = CDI.current()
                    .select(AccountSyncEventListener.class)
                    .get();

            listener.onAccountDeleted(account);
        } catch (Exception e) {
            log.error("Error in PreRemove callback for account {}", account.getId(), e);
        }
    }
}
