package org.walrex.domain.exception;

import lombok.Getter;

/**
 * Excepción lanzada cuando se intenta crear/actualizar una categoría de unidad de medida
 * con datos que ya existen en otra categoría (código o nombre duplicado).
 * Se traduce a HTTP 409 Conflict.
 */
@Getter
public class DuplicateProductCategoryUomException extends RuntimeException {

    private final String field;
    private final String value;

    /**
     * Constructor con campo y valor duplicado.
     *
     * @param field Campo duplicado (ej: "code", "name")
     * @param value Valor duplicado que causó el conflicto
     */
    public DuplicateProductCategoryUomException(String field, String value) {
        super(String.format("ProductCategoryUom with %s '%s' already exists", field, value));
        this.field = field;
        this.value = value;
    }
}
