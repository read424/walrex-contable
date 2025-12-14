package org.walrex.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO para crear una nueva moneda.
 *
 * Las validaciones aquí son de FORMATO, no de negocio.
 * La validación de unicidad se hace en el Service.
*/
public record CreateCurrencyRequest(
    /**
     * Código alfabético ISO 4217.
     * - Exactamente 3 letras
     * - Se convertirá a mayúsculas automáticamente
     */
    @NotBlank(message = "Alphabetic code is required")
    @Size(min = 3, max = 3, message = "Alphabetic code must be exactly 3 characters")
    @Pattern(
            regexp = "^[A-Za-z]{3}$",
            message = "Alphabetic code must contain only letters (A-Z)"
    )
    String alphabeticCode,

    /**
     * Código numérico ISO 4217.
     * - Exactamente 3 dígitos
     */
    @NotBlank(message = "Numeric code is required")
    @Size(min = 3, max = 3, message = "Numeric code must be exactly 3 digits")
    @Pattern(
            regexp = "^[0-9]{3}$",
            message = "Numeric code must contain only digits (0-9)"
    )
    String numericCode,

    /**
     * Nombre descriptivo de la moneda.
     */
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    String name

) {
    /**
     * Constructor que normaliza los datos.
     */
    public CreateCurrencyRequest {
        if (alphabeticCode != null) {
            alphabeticCode = alphabeticCode.toUpperCase().trim();
        }
        if (numericCode != null) {
            numericCode = numericCode.trim();
        }
        if (name != null) {
            name = name.trim();
        }
    }
}
