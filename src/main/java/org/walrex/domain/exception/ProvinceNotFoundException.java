package org.walrex.domain.exception;

import lombok.Getter;

@Getter
public class ProvinceNotFoundException extends RuntimeException {
    private final Integer idProvince;

    public ProvinceNotFoundException(Integer idProvince) {
        super("Provincia not found with id: " + idProvince);
        this.idProvince = idProvince;
    }

    public ProvinceNotFoundException(String field, String value) {
        super("Provincia not found with " + field + ": " + value);
        this.idProvince = null;
    }

    public ProvinceNotFoundException(String message) {
        super(message);
        this.idProvince = null;
    }
}
