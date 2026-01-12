package org.walrex.domain.exception;

/**
 * Excepción lanzada cuando se intenta crear/actualizar una plantilla de producto
 * con una referencia interna que ya existe.
 */
public class DuplicateProductTemplateException extends RuntimeException {

    public DuplicateProductTemplateException(String internalReference) {
        super("La referencia interna ya existe: " + internalReference);
    }

    public DuplicateProductTemplateException(String field, String value) {
        super("Ya existe una plantilla de producto con " + field + ": " + value);
    }
}
