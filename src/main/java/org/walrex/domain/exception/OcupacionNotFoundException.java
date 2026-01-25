package org.walrex.domain.exception;

import java.io.Serial;

public class OcupacionNotFoundException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public OcupacionNotFoundException() {
        super("Occupation not found.");
    }

    public OcupacionNotFoundException(String message) {
        super(message);
    }
}
