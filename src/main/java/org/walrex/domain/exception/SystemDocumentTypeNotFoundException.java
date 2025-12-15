package org.walrex.domain.exception;

/**
 * Excepción lanzada cuando no se encuentra un tipo de documento del sistema.
 */
public class SystemDocumentTypeNotFoundException extends RuntimeException {

    public SystemDocumentTypeNotFoundException(Long id) {
        super("System document type not found with id: " + id);
    }

    public SystemDocumentTypeNotFoundException(String message) {
        super(message);
    }
}
