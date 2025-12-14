package org.walrex.domain.exception;

/**
 * Exception thrown when attempting to create or update a System Document Type
 * with a code or name that already exists.
 */
public class DuplicateSystemDocumentTypeException extends RuntimeException {

    public DuplicateSystemDocumentTypeException(String field, String value) {
        super(String.format("System document type already exists with %s: %s", field, value));
    }

    public DuplicateSystemDocumentTypeException(String message) {
        super(message);
    }
}
