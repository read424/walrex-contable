package org.walrex.infrastructure.adapter.outbound.azure.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO que representa un valor de moneda extraído de un documento.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AzureCurrencyValue {

    /**
     * Monto.
     */
    private BigDecimal amount;

    /**
     * Código de moneda (USD, EUR, PEN, etc.).
     */
    private String currencyCode;

    /**
     * Símbolo de moneda ($, €, S/, etc.).
     */
    private String currencySymbol;
}
