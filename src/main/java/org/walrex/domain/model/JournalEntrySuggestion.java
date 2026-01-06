package org.walrex.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Respuesta completa del RAG Orchestrator con sugerencias de asiento contable.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JournalEntrySuggestion {

    /**
     * Fecha sugerida para el asiento.
     */
    private LocalDate suggestedDate;

    /**
     * Descripción general sugerida para el asiento.
     */
    private String suggestedDescription;

    /**
     * Tipo de libro contable sugerido.
     */
    private AccountingBookType suggestedBookType;

    /**
     * Lista de líneas de asiento sugeridas.
     */
    private List<JournalEntryLine> suggestedLines;

    /**
     * Total débito de las líneas sugeridas.
     */
    private BigDecimal totalDebit;

    /**
     * Total crédito de las líneas sugeridas.
     */
    private BigDecimal totalCredit;

    /**
     * Indica si el asiento sugerido está balanceado.
     */
    private Boolean isBalanced;

    /**
     * Contexto recuperado de Qdrant (chunks de cuentas + asientos históricos).
     */
    private RetrievedContext retrievedContext;

    /**
     * Explicación general del LLM sobre el asiento.
     */
    private String llmExplanation;

    /**
     * Proveedor LLM utilizado para generar la sugerencia.
     */
    private String llmProviderUsed;

    /**
     * Score de confianza general (0.0 a 1.0).
     */
    private Float overallConfidence;
}
