package org.walrex.domain.exception;

/**
 * Excepción lanzada cuando el documento no puede ser leído o procesado correctamente.
 */
public class DocumentUnreadableException extends DocumentAnalysisException {

    public DocumentUnreadableException(String message) {
        super(message);
    }

    public DocumentUnreadableException(String message, Throwable cause) {
        super(message, cause);
    }
}
