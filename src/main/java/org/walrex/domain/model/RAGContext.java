package org.walrex.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Contexto de entrada para el RAG Orchestrator.
 * Contiene información del documento analizado y metadata para generar sugerencias.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RAGContext {

    /**
     * Resultado del análisis del documento (Azure Document Intelligence).
     */
    private DocumentAnalysisResult documentAnalysis;

    /**
     * Fecha del asiento contable a generar.
     */
    private LocalDate entryDate;

    /**
     * Tipo de libro contable (DIARIO, COMPRAS, VENTAS).
     */
    private AccountingBookType bookType;

    /**
     * Proveedor LLM a utilizar ("groq" o "ollama"). Si es null, usa el default.
     */
    private String llmProvider;

    /**
     * Límite de resultados para búsqueda vectorial de cuentas.
     */
    @Builder.Default
    private Integer accountSearchLimit = 5;

    /**
     * Límite de resultados para búsqueda vectorial de asientos históricos.
     */
    @Builder.Default
    private Integer historicalEntrySearchLimit = 3;

    /**
     * Query de búsqueda personalizado (opcional). Si es null, se genera automáticamente.
     */
    private String customSearchQuery;
}
