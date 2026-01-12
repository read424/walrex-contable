package org.walrex.domain.exception;

/**
 * Excepción lanzada cuando una plantilla de producto no cumple con las reglas de validación.
 */
public class InvalidProductTemplateException extends RuntimeException {

    public InvalidProductTemplateException(String message) {
        super(message);
    }

    public InvalidProductTemplateException(String message, Throwable cause) {
        super(message, cause);
    }
}
