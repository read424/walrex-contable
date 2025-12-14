package org.walrex.domain.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Excepción lanzada cuando se intenta crear/actualizar una moneda
 * con datos que ya existen en otra moneda.
 * Se traduce a HTTP 409 Conflict.
*/
@Getter
public class DuplicateCurrencyException extends RuntimeException {

    private final String field;
    private final String value;

    public DuplicateCurrencyException(String field, String value){
        super(String.format("Currency with %s '%s' already exists", field, value));
        this.field = field;
        this.value = value;
    }
}
