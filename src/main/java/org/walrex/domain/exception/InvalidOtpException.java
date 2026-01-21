package org.walrex.domain.exception;

public class InvalidOtpException extends RuntimeException {
    public InvalidOtpException(String message) {
        super(message);
    }

    public InvalidOtpException() {
        super("OTP inválido o expirado");
    }
}
