package org.walrex.domain.exception;

import lombok.Getter;

/**
 * Excepción lanzada cuando no se encuentra una cuenta contable.
 * Se traduce a HTTP 404 Not Found.
 */
@Getter
public class AccountingAccountNotFoundException extends RuntimeException {

    private final Integer accountId;

    /**
     * Constructor con ID de cuenta.
     *
     * @param id ID de la cuenta no encontrada
     */
    public AccountingAccountNotFoundException(Integer id) {
        super("AccountingAccount not found with id: " + id);
        this.accountId = id;
    }

    /**
     * Constructor con mensaje personalizado.
     *
     * @param message Mensaje de error personalizado
     */
    public AccountingAccountNotFoundException(String message) {
        super(message);
        this.accountId = null;
    }
}
