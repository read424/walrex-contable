package org.walrex.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO para crear un nuevo país.
 *
 * Las validaciones aquí son de FORMATO, no de negocio.
 * La validación de unicidad se hace en el Service.
 */
public record CreateCountryRequest (
        /**
         * Código alfabético ISO 3166.
         * - Exactamente 3 letras
         * - Se convertirá a mayúsculas automáticamente
         */
        @NotBlank(message = "Alphabetic code is required")
        @Size(min = 3, max = 3, message = "Alphabetic code must be exactly 3 characters")
        @Pattern(
                regexp = "^[A-Za-z]{3}$",
                message = "Alphabetic code mus contain only letters (A-Z)"
        )
        String alphabeticCode,
        /**
         * Código numérico ISO 3166.
         * - Exactamente 3 dígitos
         */
        @NotBlank(message = "Numeric code is required")
        @Size(min = 3, max = 3, message = "Numeric code must be exactly 3 digits")
        @Pattern(
                regexp = "^[0-9]{3}$",
                message = "Numeric code must contain only digits (0-9)"
        )
        Integer numericCode,
        /**
         * Nombre descriptivo de la moneda.
         */
        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
        String name
){}
