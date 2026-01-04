package org.walrex.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Resultado del análisis de un documento procesado por Azure Document Intelligence.
 * Contiene tanto el texto extraído completo como los campos estructurados del invoice.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentAnalysisResult {

    /**
     * Texto completo extraído del documento (analyzeResult.content).
     */
    private String content;

    /**
     * Campos estructurados extraídos del documento tipo invoice.
     */
    private InvoiceField invoiceFields;

    /**
     * Número de páginas procesadas.
     */
    private Integer pageCount;

    /**
     * Modelo utilizado para el análisis (ejemplo: "prebuilt-invoice").
     */
    private String modelId;

    /**
     * Indica si el análisis fue exitoso.
     */
    private Boolean successful;

    /**
     * Mensaje de error si el análisis falló.
     */
    private String errorMessage;
}
