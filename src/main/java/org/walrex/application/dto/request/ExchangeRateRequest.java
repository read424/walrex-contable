package org.walrex.application.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * DTO para solicitar cálculo de cambio de divisas.
 *
 * Las validaciones aquí son de FORMATO, no de negocio.
 * La validación de existencia de rutas se hace en el Service.
 */
public record ExchangeRateRequest(
    /**
     * Monto a convertir en la moneda base.
     */
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    BigDecimal amount,

    /**
     * Código de la moneda base (origen).
     * Ejemplo: PEN, USD, VES
     */
    @NotBlank(message = "Base currency is required")
    @Size(min = 3, max = 3, message = "Base currency must be exactly 3 characters")
    @Pattern(
            regexp = "^[A-Z]{3}$",
            message = "Base currency must be a valid 3-letter code (uppercase)"
    )
    String baseCurrency,

    /**
     * Código de la moneda cotizada (destino).
     * Ejemplo: PEN, USD, VES
     */
    @NotBlank(message = "Quote currency is required")
    @Size(min = 3, max = 3, message = "Quote currency must be exactly 3 characters")
    @Pattern(
            regexp = "^[A-Z]{3}$",
            message = "Quote currency must be a valid 3-letter code (uppercase)"
    )
    String quoteCurrency,

    /**
     * Margen de ganancia a aplicar (opcional).
     * Formato: 5.0 = 5%
     * Si no se especifica, se usa 5.0 por defecto.
     */
    @DecimalMin(value = "0.0", message = "Margin must be 0 or greater")
    BigDecimal margin
) {
    /**
     * Constructor que normaliza los datos.
     */
    public ExchangeRateRequest {
        if (baseCurrency != null) {
            baseCurrency = baseCurrency.toUpperCase().trim();
        }
        if (quoteCurrency != null) {
            quoteCurrency = quoteCurrency.toUpperCase().trim();
        }
        // Margen por defecto: 5%
        if (margin == null) {
            margin = BigDecimal.valueOf(5.0);
        }
    }
}
