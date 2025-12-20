package org.walrex.domain.exception;

import lombok.Getter;

@Getter
public class DuplicateDepartamentException extends RuntimeException {

    private final String field;
    private final String value;

    public DuplicateDepartamentException(String field, String value) {
        super(String.format("Departament with %s '%s' already exists", field, value));
        this.field = field;
        this.value = value;
    }
}
