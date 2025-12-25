package org.walrex.domain.model;

/**
 * Enum representing the type of accounting book (libro contable).
 * This corresponds to the PostgreSQL enum 'accounting_book_type'.
 */
public enum AccountingBookType {
    /**
     * General journal (Libro Diario).
     */
    DIARIO,

    /**
     * Sales journal (Libro de Ventas).
     */
    VENTAS,

    /**
     * Purchase journal (Libro de Compras).
     */
    COMPRAS;

    /**
     * Creates an AccountingBookType from a string value.
     *
     * @param value the string value
     * @return the corresponding AccountingBookType
     * @throws IllegalArgumentException if the value is invalid
     */
    public static AccountingBookType fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Accounting book type cannot be null");
        }

        try {
            return AccountingBookType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                "Invalid accounting book type: " + value + ". Valid values are: DIARIO, VENTAS, COMPRAS"
            );
        }
    }
}
