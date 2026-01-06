package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.JournalEntrySuggestion;
import org.walrex.domain.model.RAGContext;

/**
 * Puerto de entrada para generar sugerencias de asientos contables usando RAG.
 */
public interface GenerateJournalEntrySuggestionsUseCase {

    /**
     * Genera sugerencias de asiento contable basadas en el contexto proporcionado.
     *
     * @param context Contexto con documento analizado y metadata
     * @return Uni con la sugerencia de asiento completa
     */
    Uni<JournalEntrySuggestion> generateSuggestions(RAGContext context);
}
