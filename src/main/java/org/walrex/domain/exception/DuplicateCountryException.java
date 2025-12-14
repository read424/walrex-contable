package org.walrex.domain.exception;

import lombok.Getter;

@Getter
public class DuplicateCountryException extends RuntimeException {

    private final String field;
    private final String value;

    public DuplicateCountryException(String field, String value) {
        super(String.format("Country with %s '%s' already exists", field, value));
        this.field=field;
        this.value=value;
    }
}
