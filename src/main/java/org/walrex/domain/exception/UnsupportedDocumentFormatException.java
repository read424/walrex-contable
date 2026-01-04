package org.walrex.domain.exception;

/**
 * Excepción lanzada cuando el formato del documento no está soportado.
 */
public class UnsupportedDocumentFormatException extends DocumentAnalysisException {

    private final String contentType;

    public UnsupportedDocumentFormatException(String contentType) {
        super(String.format("Formato de documento no soportado: %s. Formatos válidos: PDF, JPEG, PNG", contentType));
        this.contentType = contentType;
    }

    public String getContentType() {
        return contentType;
    }
}
