package org.walrex.domain.exception;

import lombok.Getter;

/**
 * Excepción lanzada cuando no se encuentra una categoría de unidad de medida.
 * Se traduce a HTTP 404 Not Found.
 */
@Getter
public class ProductCategoryUomNotFoundException extends RuntimeException {

    private final Integer categoryId;

    /**
     * Constructor con ID de categoría.
     *
     * @param id ID de la categoría no encontrada
     */
    public ProductCategoryUomNotFoundException(Integer id) {
        super("ProductCategoryUom not found with id: " + id);
        this.categoryId = id;
    }

    /**
     * Constructor con mensaje personalizado.
     *
     * @param message Mensaje de error personalizado
     */
    public ProductCategoryUomNotFoundException(String message) {
        super(message);
        this.categoryId = null;
    }
}
