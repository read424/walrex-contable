package org.walrex.domain.exception;

/**
 * Excepción lanzada cuando no se encuentra una plantilla de producto.
 */
public class ProductTemplateNotFoundException extends RuntimeException {

    public ProductTemplateNotFoundException(Integer id) {
        super("Plantilla de producto no encontrada con ID: " + id);
    }

    public ProductTemplateNotFoundException(String message) {
        super(message);
    }
}
