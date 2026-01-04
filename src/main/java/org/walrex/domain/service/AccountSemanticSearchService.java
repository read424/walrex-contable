package org.walrex.domain.service;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.walrex.application.port.input.SearchAccountsUseCase;
import org.walrex.application.port.output.VectorStorePort;
import org.walrex.domain.model.AccountSearchResult;
import org.walrex.infrastructure.adapter.logging.LogExecutionTime;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio de dominio para búsqueda semántica de cuentas contables.
 * Utiliza embeddings vectoriales para encontrar cuentas relevantes.
 */
@Slf4j
@ApplicationScoped
public class AccountSemanticSearchService implements SearchAccountsUseCase {

    @Inject
    EmbeddingGeneratorService embeddingGeneratorService;

    @Inject
    VectorStorePort vectorStorePort;

    @Override
    @WithSpan("AccountSemanticSearchService.searchAccounts")
    @LogExecutionTime(value = LogExecutionTime.LogLevel.INFO, logParameters = true)
    public Uni<List<AccountSearchResult>> searchAccounts(String query, int limit) {
        log.info("Searching accounts with query: '{}', limit: {}", query, limit);

        return embeddingGeneratorService.generate(query)
                .chain(queryEmbedding -> vectorStorePort.searchSimilar(queryEmbedding, limit))
                .onItem().invoke(results ->
                        log.info("Found {} results for query: '{}'", results.size(), query)
                )
                .onFailure().invoke(throwable ->
                        log.error("Error searching accounts for query: '{}'", query, throwable)
                );
    }

    @Override
    @WithSpan("AccountSemanticSearchService.searchAccountsByType")
    @LogExecutionTime(value = LogExecutionTime.LogLevel.INFO, logParameters = true)
    public Uni<List<AccountSearchResult>> searchAccountsByType(String query, String type, int limit) {
        log.info("Searching accounts with query: '{}', type: '{}', limit: {}", query, type, limit);

        Map<String, Object> filters = new HashMap<>();
        filters.put("type", type);
        filters.put("active", true);

        return embeddingGeneratorService.generate(query)
                .chain(queryEmbedding ->
                        vectorStorePort.searchSimilarWithFilters(queryEmbedding, limit, filters)
                )
                .onItem().invoke(results ->
                        log.info("Found {} results for query: '{}' with type: '{}'",
                                results.size(), query, type)
                )
                .onFailure().invoke(throwable ->
                        log.error("Error searching accounts for query: '{}', type: '{}'",
                                query, type, throwable)
                );
    }
}
