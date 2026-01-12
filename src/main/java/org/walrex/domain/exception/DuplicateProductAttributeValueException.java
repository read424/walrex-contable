package org.walrex.domain.exception;

/**
 * Excepción lanzada cuando se intenta crear un valor de atributo de producto duplicado.
 *
 * Esta es una excepción de dominio que indica que ya existe un valor de atributo
 * con el mismo ID o la misma combinación de (attributeId, name).
 */
public class DuplicateProductAttributeValueException extends RuntimeException {

    /**
     * Constructor privado. Usar métodos factory estáticos.
     *
     * @param message Mensaje de error
     */
    private DuplicateProductAttributeValueException(String message) {
        super(message);
    }

    /**
     * Crea una excepción para ID duplicado.
     *
     * @param id ID duplicado
     * @return Excepción configurada
     */
    public static DuplicateProductAttributeValueException withId(String id) {
        return new DuplicateProductAttributeValueException(
            "ProductAttributeValue with id '" + id + "' already exists"
        );
    }

    /**
     * Crea una excepción para combinación duplicada de attributeId y name.
     *
     * @param attributeId ID del atributo
     * @param name Nombre del valor
     * @return Excepción configurada
     */
    public static DuplicateProductAttributeValueException withAttributeIdAndName(Integer attributeId, String name) {
        return new DuplicateProductAttributeValueException(
            "ProductAttributeValue with attributeId '" + attributeId + "' and name '" + name + "' already exists"
        );
    }
}
