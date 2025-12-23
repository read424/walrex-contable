package org.walrex.domain.exception;

public class CategoryProductNotFoundException extends RuntimeException {
    public CategoryProductNotFoundException(String message) {
        super(message);
    }
}
