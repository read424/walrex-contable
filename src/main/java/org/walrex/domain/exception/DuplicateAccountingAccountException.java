package org.walrex.domain.exception;

import lombok.Getter;

/**
 * Excepción lanzada cuando se intenta crear/actualizar una cuenta contable
 * con datos que ya existen en otra cuenta (código o nombre duplicado).
 * Se traduce a HTTP 409 Conflict.
 */
@Getter
public class DuplicateAccountingAccountException extends RuntimeException {

    private final String field;
    private final String value;

    /**
     * Constructor con campo y valor duplicado.
     *
     * @param field Campo duplicado (ej: "code", "name")
     * @param value Valor duplicado que causó el conflicto
     */
    public DuplicateAccountingAccountException(String field, String value) {
        super(String.format("AccountingAccount with %s '%s' already exists", field, value));
        this.field = field;
        this.value = value;
    }
}
