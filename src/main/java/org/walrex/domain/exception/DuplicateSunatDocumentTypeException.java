package org.walrex.domain.exception;

/**
 * Excepción lanzada cuando se intenta crear o actualizar un tipo de documento SUNAT
 * con un valor que ya existe en otro registro.
 *
 * Esta es una excepción de dominio que indica violación de restricciones de unicidad.
 * Típicamente resulta en un HTTP 409 Conflict.
 */
public class DuplicateSunatDocumentTypeException extends RuntimeException {

    private final String field;
    private final String value;

    public DuplicateSunatDocumentTypeException(String field, String value) {
        super(String.format("SUNAT document type already exists with %s: %s", field, value));
        this.field = field;
        this.value = value;
    }

    public String getField() {
        return field;
    }

    public String getValue() {
        return value;
    }
}
