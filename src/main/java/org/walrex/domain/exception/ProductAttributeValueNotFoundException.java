package org.walrex.domain.exception;

/**
 * Excepción lanzada cuando no se encuentra un valor de atributo de producto.
 *
 * Esta es una excepción de dominio que indica que se intentó acceder
 * a un valor de atributo que no existe o está eliminado.
 */
public class ProductAttributeValueNotFoundException extends RuntimeException {

    /**
     * Crea una excepción con el ID del valor de atributo no encontrado.
     *
     * @param id ID del valor de atributo que no se encontró
     */
    public ProductAttributeValueNotFoundException(Integer id) {
        super("ProductAttributeValue not found with id: " + id);
    }

    /**
     * Crea una excepción con un mensaje personalizado.
     *
     * @param message Mensaje descriptivo del error
     */
    public ProductAttributeValueNotFoundException(String message, boolean customMessage) {
        super(message);
    }
}
