package org.walrex.domain.exception;

import java.io.Serial;

public class DuplicateOcupacionException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public DuplicateOcupacionException() {
        super("Duplicate occupation found.");
    }

    public DuplicateOcupacionException(String message) {
        super(message);
    }
}
