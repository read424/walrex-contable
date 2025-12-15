package org.walrex.domain.exception;

/**
 * Excepción lanzada cuando se intenta crear un tipo de documento duplicado.
 */
public class DuplicateSystemDocumentTypeException extends RuntimeException {

    public DuplicateSystemDocumentTypeException(String field, String value) {
        super(String.format("System document type already exists with %s: %s", field, value));
    }

    public DuplicateSystemDocumentTypeException(String message) {
        super(message);
    }
}
