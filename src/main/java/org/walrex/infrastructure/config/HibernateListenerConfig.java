package org.walrex.infrastructure.config;

import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.*;
import org.hibernate.internal.SessionFactoryImpl;
import org.walrex.infrastructure.adapter.outbound.listener.AccountSyncEventListener;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.AccountingAccountEntity;

/**
 * Configuración de listeners de Hibernate para sincronización automática de embeddings.
 * Registra listeners para eventos de persistencia (insert, update, delete).
 */
@Slf4j
@Startup
@ApplicationScoped
public class HibernateListenerConfig {

    @Inject
    AccountSyncEventListener accountSyncEventListener;

    @Inject
    EntityManager entityManager;

    /**
     * Registra los event listeners de Hibernate.
     * Se ejecuta al inicio de la aplicación.
     */
    public void registerListeners() {
        try {
            SessionFactoryImpl sessionFactory = entityManager
                    .unwrap(org.hibernate.SessionFactory.class)
                    .unwrap(SessionFactoryImpl.class);

            EventListenerRegistry registry = sessionFactory
                    .getServiceRegistry()
                    .getService(EventListenerRegistry.class);

            // Registrar listener para eventos PostInsert
            registry.getEventListenerGroup(EventType.POST_INSERT).appendListener(
                    (PostInsertEventListener) event -> {
                        if (event.getEntity() instanceof AccountingAccountEntity account) {
                            accountSyncEventListener.onAccountCreated(account);
                        }
                    }
            );

            // Registrar listener para eventos PostUpdate
            registry.getEventListenerGroup(EventType.POST_UPDATE).appendListener(
                    (PostUpdateEventListener) event -> {
                        if (event.getEntity() instanceof AccountingAccountEntity account) {
                            accountSyncEventListener.onAccountUpdated(account);
                        }
                    }
            );

            // Registrar listener para eventos PreDelete (antes de eliminar)
            registry.getEventListenerGroup(EventType.PRE_DELETE).appendListener(
                    (PreDeleteEventListener) event -> {
                        if (event.getEntity() instanceof AccountingAccountEntity account) {
                            accountSyncEventListener.onAccountDeleted(account);
                        }
                        return false;
                    }
            );

            log.info("Hibernate event listeners registered successfully for AccountingAccountEntity");

        } catch (Exception e) {
            log.error("Failed to register Hibernate event listeners", e);
        }
    }
}
