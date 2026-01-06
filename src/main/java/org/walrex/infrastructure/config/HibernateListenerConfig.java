package org.walrex.infrastructure.config;

import lombok.extern.slf4j.Slf4j;

/**
 * DEPRECATED: Esta clase ya no es necesaria.
 *
 * Problema original:
 * - Intentaba usar EntityManager (JPA tradicional/bloqueante) con Hibernate Reactive
 * - Hibernate Reactive no provee EntityManager, solo Mutiny.SessionFactory
 * - Causaba error: "Unsatisfied dependency for type jakarta.persistence.EntityManager"
 *
 * Solución implementada:
 * - Se migró a usar @EntityListeners(AccountEntityListener.class) en AccountingAccountEntity
 * - Los callbacks JPA (@PostPersist, @PostUpdate, @PreRemove) son compatibles con Hibernate Reactive
 * - Ver: AccountEntityListener.java y AccountingAccountEntity.java
 *
 * Esta clase se mantiene comentada como referencia histórica.
 * Se puede eliminar en futuras versiones.
 */
@Slf4j
public class HibernateListenerConfig {

    // COMENTADO - Ya no se usa, reemplazado por AccountEntityListener
    /*
    @Inject
    AccountSyncEventListener accountSyncEventListener;

    @Inject
    EntityManager entityManager; // NO FUNCIONA CON HIBERNATE REACTIVE

    public void registerListeners() {
        // ... implementación antigua comentada ...
    }
    */

    // Ya no se registra nada aquí - los listeners están en AccountEntityListener
}
