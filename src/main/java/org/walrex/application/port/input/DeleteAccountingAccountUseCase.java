package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;

/**
 * Puerto de entrada para eliminar y restaurar cuentas contables.
 */
public interface DeleteAccountingAccountUseCase {

    /**
     * Elimina lógicamente una cuenta (soft delete).
     *
     * La cuenta no se borra físicamente, se marca como eliminada
     * estableciendo la fecha de eliminación.
     *
     * @param id Identificador de la cuenta a eliminar
     * @return Uni con true si se eliminó correctamente, false si no se encontró
     * @throws org.walrex.domain.exception.AccountingAccountNotFoundException
     *         si no existe una cuenta activa con el ID proporcionado
     */
    Uni<Boolean> execute(Integer id);

    /**
     * Restaura una cuenta previamente eliminada.
     *
     * @param id Identificador de la cuenta a restaurar
     * @return Uni con true si se restauró correctamente, false si no se encontró
     * @throws org.walrex.domain.exception.AccountingAccountNotFoundException
     *         si no existe una cuenta eliminada con el ID proporcionado
     */
    Uni<Boolean> restore(Integer id);
}
