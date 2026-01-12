package org.walrex.domain.exception;

/**
 * Excepción lanzada cuando se intenta crear o actualizar un atributo de producto
 * con un id o nombre que ya existe en el sistema.
 */
public class DuplicateProductAttributeException extends RuntimeException {

    public DuplicateProductAttributeException(String field, String value) {
        super(String.format("ProductAttribute with %s '%s' already exists", field, value));
    }

    public DuplicateProductAttributeException(String message) {
        super(message);
    }
}
