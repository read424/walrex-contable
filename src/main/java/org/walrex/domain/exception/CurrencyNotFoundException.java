package org.walrex.domain.exception;


import lombok.Getter;

@Getter
public class CurrencyNotFoundException extends RuntimeException {

    private final Integer currencyId;

    public CurrencyNotFoundException(Integer id){
        super("Currency not found with id: "+ id);
        this.currencyId = id;
    }

    public CurrencyNotFoundException(String message){
        super(message);
        this.currencyId = null;
    }
}
