package org.walrex.domain.exception;

import lombok.Getter;

@Getter
public class DuplicateProvinceException extends RuntimeException {
    private final String field;
    private final String value;

    public DuplicateProvinceException(String field, String value) {
        super(String.format("Province with %s '%s' already exists", field, value));
        this.field = field;
        this.value = value;
    }
}
