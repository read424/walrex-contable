package org.walrex.domain.exception;

/**
 * Excepción lanzada cuando no se encuentra un atributo de producto.
 */
public class ProductAttributeNotFoundException extends RuntimeException {

    public ProductAttributeNotFoundException(String id) {
        super("ProductAttribute not found with id: " + id);
    }

    public ProductAttributeNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
