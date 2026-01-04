package org.walrex.infrastructure.adapter.outbound.azure.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * DTO que representa información de error de Azure Document Intelligence API.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AzureErrorInfo {

    /**
     * Código de error.
     */
    private String code;

    /**
     * Mensaje de error.
     */
    private String message;

    /**
     * Detalles adicionales del error.
     */
    private String target;

    /**
     * Información interna del error.
     */
    private AzureInnerError innererror;
}
