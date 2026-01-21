package org.walrex.domain.exception;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

public class DuplicateBeneficiaryAccountException extends WebApplicationException {
    public DuplicateBeneficiaryAccountException(String field, String value) {
        super("Beneficiary Account with " + field + " '" + value + "' already exists.", Response.Status.CONFLICT);
    }
}
