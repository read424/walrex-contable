package org.walrex.domain.service;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.walrex.application.dto.response.JournalLineSuggestionResponse;
import org.walrex.application.port.input.AnalyzeAndSuggestJournalLineUseCase;
import org.walrex.application.port.input.AnalyzeDocumentUseCase;
import org.walrex.application.port.input.GenerateJournalEntrySuggestionsUseCase;
import org.walrex.domain.model.AccountingBookType;
import org.walrex.domain.model.JournalEntryLine;
import org.walrex.domain.model.RAGContext;
import org.walrex.infrastructure.adapter.logging.LogExecutionTime;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio que combina análisis de documentos con RAG para sugerir líneas de asiento.
 */
@Slf4j
@ApplicationScoped
public class DocumentToJournalLineService implements AnalyzeAndSuggestJournalLineUseCase {

    @Inject
    AnalyzeDocumentUseCase analyzeDocumentUseCase;

    @Inject
    GenerateJournalEntrySuggestionsUseCase generateSuggestionsUseCase;

    @Override
    @WithSpan("DocumentToJournalLineService.analyzeAndSuggest")
    @LogExecutionTime(value = LogExecutionTime.LogLevel.INFO, logParameters = true)
    public Uni<List<JournalLineSuggestionResponse>> analyzeAndSuggest(
            FileUpload fileUpload,
            String bookType
    ) {
        log.info("Starting document analysis and journal line suggestion for: {}", fileUpload.fileName());

        // 1. Analizar documento con Azure Document Intelligence
        return analyzeDocumentUseCase.analyzeDocument(fileUpload)
                .onItem().invoke(result ->
                        log.info("Document analyzed successfully: {}", fileUpload.fileName())
                )
                // 2. Construir contexto RAG y generar sugerencias
                .chain(documentAnalysis -> {
                    RAGContext context = RAGContext.builder()
                            .documentAnalysis(documentAnalysis)
                            .bookType(parseBookType(bookType))
                            .build();

                    return generateSuggestionsUseCase.generateSuggestions(context);
                })
                // 3. Mapear a formato simplificado para frontend
                .map(suggestion -> {
                    if (suggestion.getSuggestedLines() == null || suggestion.getSuggestedLines().isEmpty()) {
                        log.warn("No suggested lines returned from RAG");
                        return Collections.<JournalLineSuggestionResponse>emptyList();
                    }

                    return suggestion.getSuggestedLines().stream()
                            .map(this::mapToResponse)
                            .collect(Collectors.toList());
                })
                .onItem().invoke(suggestions ->
                        log.info("Generated {} journal line suggestions", suggestions.size())
                )
                .onFailure().invoke(error ->
                        log.error("Error analyzing and suggesting journal lines", error)
                );
    }

    /**
     * Mapea JournalEntryLine del dominio al DTO de respuesta.
     */
    private JournalLineSuggestionResponse mapToResponse(JournalEntryLine line) {
        return JournalLineSuggestionResponse.builder()
                .accountId(line.getAccountId())
                .description(line.getDescription())
                .debit(line.getDebit() != null ? line.getDebit() : BigDecimal.ZERO)
                .credit(line.getCredit() != null ? line.getCredit() : BigDecimal.ZERO)
                // Confidence podría calcularse basado en el score del account match
                .confidence(0.8f) // Valor por defecto, se puede mejorar
                .build();
    }

    /**
     * Parse el tipo de libro de String a enum.
     */
    private AccountingBookType parseBookType(String bookType) {
        if (bookType == null || bookType.isBlank()) {
            return AccountingBookType.DIARIO;
        }

        try {
            return AccountingBookType.valueOf(bookType.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid book type: {}, defaulting to DIARIO", bookType);
            return AccountingBookType.DIARIO;
        }
    }
}
