package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.AccountingAccount;

/**
 * Puerto de entrada para actualizar cuentas contables.
 */
public interface UpdateAccountingAccountUseCase {

    /**
     * Actualiza una cuenta existente con nuevos datos.
     *
     * @param id Identificador de la cuenta a actualizar
     * @param accountingAccountingAccount Nuevos datos para la cuenta
     * @return Uni con la cuenta actualizada
     * @throws org.walrex.domain.exception.AccountingAccountNotFoundException
     *         si no existe una cuenta con el ID proporcionado
     * @throws org.walrex.domain.exception.DuplicateAccountingAccountException
     *         si los nuevos datos entran en conflicto con otra cuenta existente
     */
    Uni<AccountingAccount> execute(Integer id, AccountingAccount accountingAccountingAccount);
}
