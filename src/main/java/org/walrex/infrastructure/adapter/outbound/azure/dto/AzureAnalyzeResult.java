package org.walrex.infrastructure.adapter.outbound.azure.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * DTO que representa el resultado del análisis (analyzeResult) de Azure Document Intelligence.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AzureAnalyzeResult {

    /**
     * Versión de la API utilizada.
     */
    private String apiVersion;

    /**
     * ID del modelo utilizado para el análisis.
     */
    private String modelId;

    /**
     * Texto completo extraído del documento.
     */
    private String content;

    /**
     * Páginas del documento.
     */
    private List<AzurePageInfo> pages;

    /**
     * Documentos encontrados y analizados.
     */
    private List<AzureDocument> documents;
}
