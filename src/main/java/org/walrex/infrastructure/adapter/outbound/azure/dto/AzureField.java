package org.walrex.infrastructure.adapter.outbound.azure.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * DTO que representa un campo extraído de un documento.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AzureField {

    /**
     * Tipo de campo (string, number, date, currency, etc.).
     */
    private String type;

    /**
     * Valor del campo como string.
     */
    private String valueString;

    /**
     * Valor del campo como número.
     */
    private Double valueNumber;

    /**
     * Valor del campo como fecha (formato ISO).
     */
    private String valueDate;

    /**
     * Valor del campo como objeto de moneda.
     */
    private AzureCurrencyValue valueCurrency;

    /**
     * Contenido del campo (texto tal como aparece en el documento).
     */
    private String content;

    /**
     * Nivel de confianza de la extracción (0.0 a 1.0).
     */
    private Double confidence;
}
