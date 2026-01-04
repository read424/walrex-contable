package org.walrex.domain.exception;

/**
 * Excepción base para errores relacionados con el análisis de documentos.
 */
public class DocumentAnalysisException extends RuntimeException {

    public DocumentAnalysisException(String message) {
        super(message);
    }

    public DocumentAnalysisException(String message, Throwable cause) {
        super(message, cause);
    }
}
