package org.walrex.application.port.output;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.AccountingAccount;

/**
 * Puerto de salida para consultas específicas de sincronización de cuentas.
 * Extiende las consultas existentes con métodos específicos para embeddings.
 */
public interface AccountSyncQueryPort {

    /**
     * Obtiene todas las cuentas activas que no han sido sincronizadas.
     *
     * @return Multi que emite cada cuenta no sincronizada
     */
    Multi<AccountingAccount> findUnsyncedAccounts();

    /**
     * Obtiene una cuenta por su ID.
     *
     * @param accountId ID de la cuenta
     * @return Uni con la cuenta encontrada o vacío si no existe
     */
    Uni<AccountingAccount> findById(Integer accountId);

    /**
     * Marca una cuenta como sincronizada.
     *
     * @param accountId ID de la cuenta
     * @return Uni<Void> que completa cuando se actualiza
     */
    Uni<Void> markAsSynced(Integer accountId);

    /**
     * Marca una cuenta como no sincronizada.
     * Útil para forzar resincronización.
     *
     * @param accountId ID de la cuenta
     * @return Uni<Void> que completa cuando se actualiza
     */
    Uni<Void> markAsUnsynced(Integer accountId);

    /**
     * Cuenta el número de cuentas pendientes de sincronización.
     *
     * @return Uni con el número de cuentas no sincronizadas
     */
    Uni<Long> countUnsyncedAccounts();

    /**
     * Marca todas las cuentas activas como no sincronizadas.
     * Útil para resincronización completa.
     *
     * @return Uni<Void> que completa cuando todas son marcadas
     */
    Uni<Void> markAllAsUnsynced();
}
