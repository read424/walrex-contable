package org.walrex.domain.service;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.walrex.application.port.input.GenerateJournalEntrySuggestionsUseCase;
import org.walrex.application.port.output.ChatOutputPort;
import org.walrex.application.port.output.VectorStorePort;
import org.walrex.domain.model.*;
import org.walrex.infrastructure.adapter.logging.LogExecutionTime;

import java.time.Duration;

/**
 * Servicio orquestador del flujo RAG para generar sugerencias de asientos contables.
 * Implementa el pipeline completo: extracción → embedding → búsqueda → LLM → parsing.
 */
@Slf4j
@ApplicationScoped
public class RAGOrchestratorService implements GenerateJournalEntrySuggestionsUseCase {

    @Inject
    LLMStrategyFactory llmFactory;

    @Inject
    EmbeddingGeneratorService embeddingService;

    @Inject
    VectorStorePort vectorStorePort;

    @Inject
    PromptTemplateService promptTemplateService;

    @Inject
    LLMResponseParserService responseParserService;

    @ConfigProperty(name = "rag.llm.enable-fallback", defaultValue = "true")
    Boolean enableFallback;

    @Override
    @WithSpan("RAGOrchestratorService.generateSuggestions")
    @LogExecutionTime(value = LogExecutionTime.LogLevel.INFO, logParameters = true)
    public Uni<JournalEntrySuggestion> generateSuggestions(RAGContext context) {
        log.info("Starting RAG orchestration for document analysis");

        // 1. Extraer contexto del documento
        String searchQuery = extractSearchQuery(context);
        log.debug("Search query generated: {}", searchQuery);

        // 2. Generar embedding del query
        return embeddingService.generate(searchQuery)
                // 3. Búsqueda vectorial híbrida en Qdrant
                .chain(queryEmbedding -> {
                    log.debug("Embedding generated, performing hybrid search");
                    return searchVectorStore(queryEmbedding, context)
                            .map(hybridResult -> {
                                // Construir RetrievedContext
                                return RetrievedContext.builder()
                                        .similarAccounts(hybridResult.getAccounts())
                                        .similarHistoricalEntries(hybridResult.getHistoricalEntries())
                                        .searchQuery(searchQuery)
                                        .queryEmbedding(queryEmbedding)
                                        .build();
                            });
                })
                // 4. Construir prompt con contexto recuperado y generar con LLM
                .chain(retrievedContext -> {
                    log.debug("Building prompts and generating with LLM");
                    return buildPromptAndGenerate(context, retrievedContext);
                })
                // 5. Parsear respuesta del LLM a JournalEntrySuggestion
                .chain(llmResponseWithContext -> {
                    log.debug("Parsing LLM response");
                    return responseParserService.parseToJournalEntrySuggestion(
                            llmResponseWithContext.response,
                            context,
                            llmResponseWithContext.context,
                            llmResponseWithContext.provider != null ? llmResponseWithContext.provider : "groq"
                    );
                })
                .onItem().invoke(suggestion ->
                        log.info("RAG orchestration completed successfully with confidence: {}",
                                suggestion.getOverallConfidence())
                )
                .onFailure().invoke(throwable ->
                        log.error("RAG orchestration failed", throwable)
                );
    }

    /**
     * Extrae el query de búsqueda del contexto del documento.
     */
    private String extractSearchQuery(RAGContext context) {
        // Si hay query personalizado, usarlo
        if (context.getCustomSearchQuery() != null && !context.getCustomSearchQuery().isBlank()) {
            return context.getCustomSearchQuery();
        }

        // Generar query a partir del documento
        StringBuilder query = new StringBuilder();

        if (context.getDocumentAnalysis() != null &&
                context.getDocumentAnalysis().getInvoiceFields() != null) {
            InvoiceField invoice = context.getDocumentAnalysis().getInvoiceFields();

            if (invoice.getVendorName() != null) {
                query.append("Proveedor: ").append(invoice.getVendorName()).append(". ");
            }

            if (invoice.getTotalAmount() != null) {
                query.append("Monto: ").append(invoice.getTotalAmount()).append(". ");
            }

            // Agregar tipo de libro
            if (context.getBookType() != null) {
                query.append("Libro: ").append(context.getBookType()).append(". ");
            }

            // Agregar descripción general si existe
            if (context.getDocumentAnalysis().getContent() != null &&
                    !context.getDocumentAnalysis().getContent().isBlank()) {
                // Tomar primeras 200 caracteres del contenido
                String content = context.getDocumentAnalysis().getContent();
                if (content.length() > 200) {
                    content = content.substring(0, 200);
                }
                query.append(content);
            }
        }

        String result = query.toString().trim();
        return result.isEmpty() ? "Factura de compra" : result;
    }

    /**
     * Realiza búsqueda híbrida en Qdrant.
     */
    private Uni<HybridSearchResult> searchVectorStore(float[] queryEmbedding, RAGContext context) {
        int accountLimit = context.getAccountSearchLimit() != null ?
                context.getAccountSearchLimit() : 5;
        int historicalLimit = context.getHistoricalEntrySearchLimit() != null ?
                context.getHistoricalEntrySearchLimit() : 3;

        return vectorStorePort.searchHybrid(queryEmbedding, accountLimit, historicalLimit);
    }

    /**
     * Construye prompt y genera respuesta con LLM seleccionado.
     */
    private Uni<LLMResponseWithContext> buildPromptAndGenerate(RAGContext context, RetrievedContext retrievedContext) {
        String systemPrompt = promptTemplateService.buildSystemPrompt();
        String userPrompt = promptTemplateService.buildUserPrompt(context, retrievedContext);

        // Seleccionar LLM
        String providerName = context.getLlmProvider();
        ChatOutputPort llm = providerName != null && !providerName.isBlank() ?
                llmFactory.getLLM(providerName) :
                llmFactory.getDefaultLLM();

        log.info("Using LLM provider: {}",
                providerName != null ? providerName : "default");

        // Generar con retry y fallback
        return llm.generateResponse(systemPrompt, userPrompt)
                .onFailure().retry().withBackOff(Duration.ofSeconds(1)).atMost(2)
                .onFailure().recoverWithUni(error -> {
                    if (enableFallback && !"ollama".equalsIgnoreCase(providerName)) {
                        log.warn("Primary LLM failed, falling back to Ollama", error);
                        return llmFactory.getLLM("ollama")
                                .generateResponse(systemPrompt, userPrompt);
                    }
                    return Uni.createFrom().failure(error);
                })
                .map(response -> new LLMResponseWithContext(response, retrievedContext, providerName));
    }

    /**
     * Clase auxiliar interna para pasar la respuesta del LLM junto con el contexto.
     */
    private static class LLMResponseWithContext {
        final String response;
        final RetrievedContext context;
        final String provider;

        LLMResponseWithContext(String response, RetrievedContext context, String provider) {
            this.response = response;
            this.context = context;
            this.provider = provider;
        }
    }
}
