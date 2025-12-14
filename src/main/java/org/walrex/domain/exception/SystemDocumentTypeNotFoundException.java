package org.walrex.domain.exception;

/**
 * Exception thrown when a requested System Document Type is not found.
 */
public class SystemDocumentTypeNotFoundException extends RuntimeException {

    public SystemDocumentTypeNotFoundException(Long id) {
        super("System document type not found with id: " + id);
    }

    public SystemDocumentTypeNotFoundException(String message) {
        super(message);
    }
}
