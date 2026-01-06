package org.walrex.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Chunk de un asiento contable histórico almacenado en Qdrant.
 * Se usa para recuperar asientos similares y aprender de patrones históricos.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoricalEntryChunk {

    /**
     * ID del asiento contable original.
     */
    private Integer journalEntryId;

    /**
     * Fecha del asiento.
     */
    private LocalDate entryDate;

    /**
     * Descripción del asiento.
     */
    private String description;

    /**
     * Tipo de libro contable.
     */
    private AccountingBookType bookType;

    /**
     * Texto del chunk formateado para embeddings.
     * Formato: "Fecha: [date]. Tipo: [bookType]. Descripción: [description].
     *          Líneas: [line1], [line2], ..."
     */
    private String chunkText;

    /**
     * Embedding vectorial del chunk.
     */
    private float[] embedding;

    /**
     * Total débito del asiento.
     */
    private BigDecimal totalDebit;

    /**
     * Total crédito del asiento.
     */
    private BigDecimal totalCredit;

    /**
     * Score de similitud con el query (0.0 a 1.0).
     */
    private Float similarityScore;
}
