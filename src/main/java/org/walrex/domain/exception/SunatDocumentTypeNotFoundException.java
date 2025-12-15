package org.walrex.domain.exception;

/**
 * Excepción lanzada cuando no se encuentra un tipo de documento SUNAT.
 *
 * Esta es una excepción de dominio que indica que el recurso solicitado no existe.
 * Típicamente resulta en un HTTP 404 Not Found.
 */
public class SunatDocumentTypeNotFoundException extends RuntimeException {

    public SunatDocumentTypeNotFoundException(Integer id) {
        super("SUNAT document type not found with id: " + id);
    }

    public SunatDocumentTypeNotFoundException(String message, Object... args) {
        super(String.format(message, args));
    }
}
