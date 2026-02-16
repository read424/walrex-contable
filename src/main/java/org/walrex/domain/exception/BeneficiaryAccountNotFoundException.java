package org.walrex.domain.exception;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

public class BeneficiaryAccountNotFoundException extends WebApplicationException {
    public BeneficiaryAccountNotFoundException(Long id) {
        super("Beneficiary Account with ID " + id + " not found.", Response.Status.NOT_FOUND);
    }
}
