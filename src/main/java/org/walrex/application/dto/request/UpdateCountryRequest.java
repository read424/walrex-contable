package org.walrex.application.dto.request;

import jakarta.validation.constraints.*;

/**
 * DTO para actualizar un país existente.
 *
 * Usa las mismas validaciones que CreateCountryRequest.
 * En un PUT, todos los campos son obligatorios (reemplazo completo).
 * Para PATCH (actualización parcial) usaríamos campos opcionales.
 */
public record UpdateCountryRequest (
        @NotBlank(message = "Alphabetic code is required")
        @Size(min = 3, max = 3, message = "Alphabetic code must be exactly 3 characters")
        @Pattern(
                regexp = "^[A-Za-z]{3}$",
                message = "Alphabetic code must contain only letters (A-Z)"
        )
        String alphabeticCode,

        @NotNull(message = "Numeric code is required")
        @Positive(message = "Numeric code debe ser un numero positivo")
        @DecimalMax(inclusive = true, value="999", message = "El valor máximo es 999")
        Integer numericCode,

        @NotBlank(message = "Name is required")
        @Size(min = 3, max = 100, message = "Name must be between 3 and 100 characters")
        String name,

        @NotBlank(message = "phoneCode is required")
        @Size(min = 2, max = 3, message = "Phone code must be between 2 and 3 characters")
        String phoneCode,

        /**
         * Permite activar/desactivar el país sin eliminarlo.
         */
        Boolean active
) {
    public UpdateCountryRequest {
        if (alphabeticCode != null) {
            alphabeticCode = alphabeticCode.toUpperCase().trim();
        }
        if (name != null) {
            name = name.trim();
        }
        if (active == null) {
            active = true;
        }
    }
}
