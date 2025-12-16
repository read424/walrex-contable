package org.walrex.domain.exception;

import lombok.Getter;

@Getter
public class CustomerNotFoundException extends RuntimeException {

    private final Integer customerId;

    public CustomerNotFoundException(Integer id) {
        super("Customer not found with id: "+ id);
        this.customerId = id;
    }


    public CustomerNotFoundException(String message) {
        super(message);
        this.customerId = null;
    }
}
