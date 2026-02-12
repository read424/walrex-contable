package org.walrex.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ExchangeRateQueryRequest(
        @NotBlank @Size(min = 2, max = 2) @Pattern(regexp = "^[A-Z]{2}$") String fromCountry,
        @NotBlank @Size(min = 3, max = 3) @Pattern(regexp = "^[A-Z]{3}$") String fromCurrency,
        @NotBlank @Size(min = 2, max = 2) @Pattern(regexp = "^[A-Z]{2}$") String toCountry,
        @NotBlank @Size(min = 3, max = 3) @Pattern(regexp = "^[A-Z]{3}$") String toCurrency
) {
    public ExchangeRateQueryRequest {
        if (fromCountry != null) fromCountry = fromCountry.toUpperCase();
        if (fromCurrency != null) fromCurrency = fromCurrency.toUpperCase();
        if (toCountry != null) toCountry = toCountry.toUpperCase();
        if (toCurrency != null) toCurrency = toCurrency.toUpperCase();
    }
}
