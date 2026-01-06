package org.walrex.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Resultado de búsqueda híbrida en Qdrant (cuentas + asientos históricos).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HybridSearchResult {

    /**
     * Cuentas contables similares.
     */
    private List<AccountSearchResult> accounts;

    /**
     * Asientos históricos similares.
     */
    private List<HistoricalEntryChunk> historicalEntries;
}
