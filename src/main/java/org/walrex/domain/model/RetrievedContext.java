package org.walrex.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Contexto recuperado de Qdrant para generar sugerencias.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetrievedContext {

    /**
     * Cuentas contables recuperadas por similitud semántica.
     */
    private List<AccountSearchResult> similarAccounts;

    /**
     * Asientos históricos recuperados por similitud semántica.
     */
    private List<HistoricalEntryChunk> similarHistoricalEntries;

    /**
     * Query utilizado para la búsqueda vectorial.
     */
    private String searchQuery;

    /**
     * Embedding generado del query.
     */
    private float[] queryEmbedding;
}
