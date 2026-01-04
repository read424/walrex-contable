package org.walrex.infrastructure.adapter.outbound.azure.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * DTO que representa la respuesta completa de Azure Document Intelligence API.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AzureAnalyzeResponse {

    /**
     * Estado del análisis (succeeded, failed, running).
     */
    private String status;

    /**
     * Resultado del análisis del documento.
     */
    private AzureAnalyzeResult analyzeResult;

    /**
     * Información de error si el análisis falló.
     */
    private AzureErrorInfo error;
}
