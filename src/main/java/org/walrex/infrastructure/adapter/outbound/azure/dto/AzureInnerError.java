package org.walrex.infrastructure.adapter.outbound.azure.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * DTO que representa información de error interno de Azure.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AzureInnerError {

    /**
     * Código de error interno.
     */
    private String code;

    /**
     * Mensaje de error interno.
     */
    private String message;
}
