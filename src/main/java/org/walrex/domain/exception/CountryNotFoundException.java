package org.walrex.domain.exception;

import lombok.Getter;

@Getter
public class CountryNotFoundException extends RuntimeException {

    private final Integer countryId;

    public CountryNotFoundException(Integer id) {
        super("Country not found with id: "+ id);
        this.countryId = id;
    }

    public CountryNotFoundException(String message) {
        super(message);
        this.countryId = null;
    }
}
