package org.walrex.domain.exception;

/**
 * Excepción lanzada cuando no se encuentra una unidad de medida de producto.
 *
 * Se usa en:
 * - Búsqueda por ID que no encuentra resultado
 * - Búsqueda por código que no encuentra resultado
 * - Actualización de unidad que no existe
 * - Eliminación de unidad que no existe
 */
public class ProductUomNotFoundException extends RuntimeException {

    /**
     * Constructor con ID.
     */
    public ProductUomNotFoundException(Integer id) {
        super("ProductUom not found with id: " + id);
    }

    /**
     * Constructor con mensaje personalizado.
     */
    public ProductUomNotFoundException(String message) {
        super(message);
    }

    /**
     * Constructor con mensaje y causa.
     */
    public ProductUomNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
