package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.SyncResult;

/**
 * Puerto de entrada para sincronizar embeddings de cuentas contables.
 * Define los casos de uso para la sincronización con la base de datos vectorial.
 */
public interface SyncAccountEmbeddingsUseCase {

    /**
     * Sincroniza todas las cuentas que aún no han sido procesadas.
     * Procesa en lotes y actualiza el campo embeddingsSynced.
     *
     * @return Uni con el resultado de la sincronización
     */
    Uni<SyncResult> syncUnsyncedAccounts();

    /**
     * Sincroniza una cuenta específica por su ID.
     * Útil para resincronización manual o tras actualización de una cuenta.
     *
     * @param accountId ID de la cuenta a sincronizar
     * @return Uni<Void> que completa cuando la sincronización finaliza
     */
    Uni<Void> syncAccount(Integer accountId);

    /**
     * Elimina el embedding de una cuenta del vector store.
     * Se usa cuando una cuenta es eliminada o desactivada.
     *
     * @param accountId ID de la cuenta a remover
     * @return Uni<Void> que completa cuando la eliminación finaliza
     */
    Uni<Void> removeSyncedAccount(Integer accountId);

    /**
     * Fuerza la resincronización de todas las cuentas activas.
     * Útil para regenerar todos los embeddings tras cambios en el modelo.
     *
     * @return Uni con el resultado de la sincronización completa
     */
    Uni<SyncResult> forceResyncAll();
}
