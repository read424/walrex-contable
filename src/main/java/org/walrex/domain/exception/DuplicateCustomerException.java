package org.walrex.domain.exception;

public class DuplicateCustomerException extends RuntimeException {
    private final String field;
    private final String value;

    public DuplicateCustomerException(String field, String value){
        super(String.format("Customer with %s '%s' already exists", field, value));
        this.field = field;
        this.value = value;
    }

}
