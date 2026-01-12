package org.walrex.domain.exception;

/**
 * Excepción lanzada cuando una operación de cálculo de tipo de cambio
 * excede el tiempo máximo de espera permitido.
 *
 * Esta excepción se usa para distinguir timeouts de otros errores
 * y permitir un manejo apropiado (ej: retornar 504 Gateway Timeout).
 */
public class ExchangeRateTimeoutException extends RuntimeException {

    public ExchangeRateTimeoutException(String message) {
        super(message);
    }

    public ExchangeRateTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
