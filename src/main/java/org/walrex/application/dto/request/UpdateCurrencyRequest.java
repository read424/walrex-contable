package org.walrex.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO para actualizar una moneda existente.
 *
 * Usa las mismas validaciones que CreateCurrencyRequest.
 * En un PUT, todos los campos son obligatorios (reemplazo completo).
 * Para PATCH (actualización parcial) usaríamos campos opcionales.
 */
public record UpdateCurrencyRequest(

        @NotBlank(message = "Alphabetic code is required")
        @Size(min = 3, max = 3, message = "Alphabetic code must be exactly 3 characters")
        @Pattern(
                regexp = "^[A-Za-z]{3}$",
                message = "Alphabetic code must contain only letters (A-Z)"
        )
        String alphabeticCode,

        @NotNull(message = "Numeric code is required")
        @Size(min = 3, max = 3, message = "Numeric code must be exactly 3 digits")
        @Pattern(
                regexp = "^[0-9]{3}$",
                message = "Numeric code must contain only digits (0-9)"
        )
        String numericCode,

        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
        String name,

        /**
         * Permite activar/desactivar la moneda sin eliminarla.
         */
        Boolean active

) {
    /**
     * Constructor que normaliza los datos.
     */
    public UpdateCurrencyRequest {
        if (alphabeticCode != null) {
            alphabeticCode = alphabeticCode.trim();
        }
        if (name != null) {
            name = name.trim();
        }
        if (active == null) {
            active = true;
        }
    }
}