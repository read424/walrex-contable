package org.walrex.domain.exception;

/**
 * Excepción lanzada cuando se intenta crear o actualizar una unidad de medida
 * con un código que ya existe.
 *
 * Se usa en:
 * - Creación de nueva unidad con código duplicado
 * - Actualización de unidad con código que ya existe en otra unidad
 */
public class DuplicateProductUomException extends RuntimeException {

    /**
     * Constructor con campo y valor duplicado.
     *
     * @param field Campo que tiene valor duplicado (ej: "code")
     * @param value Valor que está duplicado
     */
    public DuplicateProductUomException(String field, String value) {
        super(String.format("ProductUom with %s '%s' already exists", field, value));
    }

    /**
     * Constructor con mensaje personalizado.
     */
    public DuplicateProductUomException(String message) {
        super(message);
    }

    /**
     * Constructor con mensaje y causa.
     */
    public DuplicateProductUomException(String message, Throwable cause) {
        super(message, cause);
    }
}
