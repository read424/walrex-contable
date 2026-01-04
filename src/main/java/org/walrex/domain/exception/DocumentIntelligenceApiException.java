package org.walrex.domain.exception;

/**
 * Excepción lanzada cuando ocurre un error en la comunicación con Azure Document Intelligence API.
 * Incluye errores de autenticación, rate limiting, timeouts, etc.
 */
public class DocumentIntelligenceApiException extends DocumentAnalysisException {

    private final Integer statusCode;
    private final String errorCode;

    public DocumentIntelligenceApiException(String message) {
        super(message);
        this.statusCode = null;
        this.errorCode = null;
    }

    public DocumentIntelligenceApiException(String message, Integer statusCode, String errorCode) {
        super(String.format("Error en Azure Document Intelligence API (status: %d, code: %s): %s",
                statusCode, errorCode, message));
        this.statusCode = statusCode;
        this.errorCode = errorCode;
    }

    public DocumentIntelligenceApiException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = null;
        this.errorCode = null;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
