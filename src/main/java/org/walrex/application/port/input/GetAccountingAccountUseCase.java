package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.AccountingAccount;

/**
 * Puerto de entrada para obtener cuentas contables.
 */
public interface GetAccountingAccountUseCase {
    /**
     * Obtiene una cuenta por su ID.
     *
     * @param id Identificador único de la cuenta
     * @return Uni con la cuenta encontrada
     * @throws org.walrex.domain.exception.AccountingAccountNotFoundException
     *         si no existe una cuenta con el ID proporcionado
     */
    Uni<AccountingAccount> findById(Integer id);

    /**
     * Obtiene una cuenta por su código único.
     *
     * @param code Código único de la cuenta (ej: "1010", "2020")
     * @return Uni con la cuenta encontrada
     * @throws org.walrex.domain.exception.AccountingAccountNotFoundException
     *         si no existe una cuenta con el código proporcionado
     */
    Uni<AccountingAccount> findByCode(String code);
}
