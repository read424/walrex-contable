package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.AccountingAccount;

/**
 * Puerto de entrada para crear cuentas contables.
 *
 * Siguiendo el patrón hexagonal, este puerto define el contrato
 * para el caso de uso de creación de cuentas.
 */
public interface CreateAccountingAccountUseCase {
    /**
     * Crea una nueva cuenta contable en el sistema.
     *
     * @param accountingAccountingAccount Datos necesarios para crear la cuenta
     * @return Uni con la cuenta creada
     * @throws org.walrex.domain.exception.DuplicateAccountingAccountException
     *         si ya existe una cuenta con el mismo código o nombre
     */
    Uni<AccountingAccount> execute(AccountingAccount accountingAccountingAccount);
}
