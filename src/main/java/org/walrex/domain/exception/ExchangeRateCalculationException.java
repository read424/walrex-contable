package org.walrex.domain.exception;

public class ExchangeRateCalculationException extends RuntimeException {
    public ExchangeRateCalculationException(String message) {
        super(message);
    }

    public ExchangeRateCalculationException(String message, Throwable cause) {
        super(message, cause);
    }
}
