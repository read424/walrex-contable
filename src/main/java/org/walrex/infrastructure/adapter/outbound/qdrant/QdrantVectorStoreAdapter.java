package org.walrex.infrastructure.adapter.outbound.qdrant;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.comparison.IsEqualTo;
import dev.langchain4j.store.embedding.filter.logical.And;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.quarkus.runtime.Startup;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.walrex.application.port.output.VectorStorePort;
import org.walrex.domain.model.AccountChunk;
import org.walrex.domain.model.AccountSearchResult;
import org.walrex.domain.model.AccountingBookType;
import org.walrex.domain.model.HistoricalEntryChunk;
import org.walrex.domain.model.HybridSearchResult;
import org.walrex.infrastructure.adapter.logging.LogExecutionTime;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Adapter que implementa el puerto VectorStorePort usando Qdrant.
 * Utiliza la librería quarkus-langchain4j-qdrant para la integración.
 */
@Slf4j
@ApplicationScoped
public class QdrantVectorStoreAdapter implements VectorStorePort {

    @Inject
    EmbeddingStore<TextSegment> embeddingStore;

    @Override
    @WithSpan("QdrantVectorStoreAdapter.upsertAccountEmbedding")
    @LogExecutionTime(value = LogExecutionTime.LogLevel.DEBUG, logParameters = true)
    public Uni<Void> upsertAccountEmbedding(AccountChunk accountChunk) {
        log.debug("Upserting embedding for account ID: {} ({})",
                accountChunk.getAccountId(), accountChunk.getCode());

        return Uni.createFrom().item(() -> {
            // Crear metadata
            Map<String, Object> metadataMap = new HashMap<>();
            metadataMap.put("chunk_type", "account");  // Diferenciar de journal entries
            metadataMap.put("account_id", accountChunk.getAccountId());
            metadataMap.put("code", accountChunk.getCode());
            metadataMap.put("name", accountChunk.getName());
            metadataMap.put("type", accountChunk.getType().name());
            metadataMap.put("normal_side", accountChunk.getNormalSide().name());
            metadataMap.put("active", String.valueOf(accountChunk.getActive()));  // Boolean -> String

            // Crear Metadata de LangChain4j
            dev.langchain4j.data.document.Metadata metadata =
                    dev.langchain4j.data.document.Metadata.from(metadataMap);

            // Crear TextSegment con el chunk text y metadata
            TextSegment textSegment = TextSegment.from(accountChunk.getChunkText(), metadata);

            // Crear embedding de LangChain4j
            dev.langchain4j.data.embedding.Embedding embedding =
                    dev.langchain4j.data.embedding.Embedding.from(accountChunk.getEmbedding());

            // Generar UUID determinístico a partir del account_id
            // Esto permite regenerar el mismo UUID para la misma cuenta
            String namespace = "account-";
            UUID pointUuid = UUID.nameUUIDFromBytes((namespace + accountChunk.getAccountId()).getBytes(StandardCharsets.UTF_8));
            String pointId = pointUuid.toString();

            // Almacenar en Qdrant (el metadata ya está en el embedding store internamente)
            embeddingStore.add(pointId, embedding);

            log.debug("Successfully upserted embedding for account: {}", accountChunk.getCode());
            return (Void) null;
        }).runSubscriptionOn(io.smallrye.mutiny.infrastructure.Infrastructure.getDefaultWorkerPool());
    }

    @Override
    @WithSpan("QdrantVectorStoreAdapter.deleteAccountEmbedding")
    @LogExecutionTime(value = LogExecutionTime.LogLevel.DEBUG, logParameters = true)
    public Uni<Void> deleteAccountEmbedding(Integer accountId) {
        log.debug("Deleting embedding for account ID: {}", accountId);

        return Uni.createFrom().item(() -> {
            // Regenerar el mismo UUID determinístico
            String namespace = "account-";
            UUID pointUuid = UUID.nameUUIDFromBytes((namespace + accountId).getBytes(StandardCharsets.UTF_8));
            String pointId = pointUuid.toString();
            embeddingStore.remove(pointId);
            log.debug("Successfully deleted embedding for account ID: {}", accountId);
            return (Void) null;
        }).runSubscriptionOn(io.smallrye.mutiny.infrastructure.Infrastructure.getDefaultWorkerPool());
    }

    @Override
    @WithSpan("QdrantVectorStoreAdapter.searchSimilar")
    @LogExecutionTime(value = LogExecutionTime.LogLevel.DEBUG, logParameters = false)
    public Uni<List<AccountSearchResult>> searchSimilar(float[] queryEmbedding, int limit) {
        log.debug("Searching similar accounts, limit: {}", limit);

        return Uni.createFrom().item(() -> {
            // Crear embedding de LangChain4j
            dev.langchain4j.data.embedding.Embedding embedding =
                    dev.langchain4j.data.embedding.Embedding.from(queryEmbedding);

            // Construir request de búsqueda
            EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                    .queryEmbedding(embedding)
                    .maxResults(limit)
                    .build();

            // Ejecutar búsqueda
            List<EmbeddingMatch<TextSegment>> matches = embeddingStore.search(searchRequest).matches();

            // Convertir a AccountSearchResult
            List<AccountSearchResult> results = matches.stream()
                    .map(this::toSearchResult)
                    .collect(Collectors.toList());

            log.debug("Found {} similar accounts", results.size());
            return results;
        }).runSubscriptionOn(io.smallrye.mutiny.infrastructure.Infrastructure.getDefaultWorkerPool());
    }

    @Override
    @WithSpan("QdrantVectorStoreAdapter.searchSimilarWithFilters")
    @LogExecutionTime(value = LogExecutionTime.LogLevel.DEBUG, logParameters = false)
    public Uni<List<AccountSearchResult>> searchSimilarWithFilters(
            float[] queryEmbedding,
            int limit,
            Map<String, Object> filters) {
        log.debug("Searching similar accounts with filters, limit: {}", limit);

        return Uni.createFrom().item(() -> {
            // Crear embedding de LangChain4j
            dev.langchain4j.data.embedding.Embedding embedding =
                    dev.langchain4j.data.embedding.Embedding.from(queryEmbedding);

            // Construir filtros de LangChain4j
            Filter filter = buildFilter(filters);

            // Construir request de búsqueda
            EmbeddingSearchRequest.EmbeddingSearchRequestBuilder requestBuilder =
                    EmbeddingSearchRequest.builder()
                            .queryEmbedding(embedding)
                            .maxResults(limit);

            if (filter != null) {
                requestBuilder.filter(filter);
            }

            // Ejecutar búsqueda
            List<EmbeddingMatch<TextSegment>> matches =
                    embeddingStore.search(requestBuilder.build()).matches();

            // Convertir a AccountSearchResult
            List<AccountSearchResult> results = matches.stream()
                    .map(this::toSearchResult)
                    .collect(Collectors.toList());

            log.debug("Found {} similar accounts with filters", results.size());
            return results;
        }).runSubscriptionOn(io.smallrye.mutiny.infrastructure.Infrastructure.getDefaultWorkerPool());
    }

    @Override
    @WithSpan("QdrantVectorStoreAdapter.collectionExists")
    public Uni<Boolean> collectionExists() {
        // La librería quarkus-langchain4j-qdrant maneja automáticamente la creación de la colección
        // Por ahora, siempre retornamos true ya que la colección se crea automáticamente
        return Uni.createFrom().item(true);
    }

    @Override
    @WithSpan("QdrantVectorStoreAdapter.createCollection")
    public Uni<Void> createCollection() {
        // La librería quarkus-langchain4j-qdrant crea automáticamente la colección
        // si no existe cuando se usa el EmbeddingStore
        log.info("Collection creation is handled automatically by quarkus-langchain4j-qdrant");
        return Uni.createFrom().voidItem();
    }

    /**
     * Convierte un EmbeddingMatch a AccountSearchResult.
     */
    private AccountSearchResult toSearchResult(EmbeddingMatch<TextSegment> match) {
        TextSegment segment = match.embedded();
        Map<String, Object> metadata = segment.metadata().toMap();

        return AccountSearchResult.builder()
                .accountId((Integer) metadata.get("account_id"))
                .code((String) metadata.get("code"))
                .name((String) metadata.get("name"))
                .type(org.walrex.domain.model.AccountType.valueOf((String) metadata.get("type")))
                .normalSide(org.walrex.domain.model.NormalSide.valueOf((String) metadata.get("normal_side")))
                .score(match.score().floatValue())
                .active((Boolean) metadata.get("active"))
                .build();
    }

    /**
     * Construye un filtro de LangChain4j a partir de un mapa de filtros.
     */
    private Filter buildFilter(Map<String, Object> filters) {
        if (filters == null || filters.isEmpty()) {
            return null;
        }

        List<Filter> filterList = new ArrayList<>();

        for (Map.Entry<String, Object> entry : filters.entrySet()) {
            filterList.add(new IsEqualTo(entry.getKey(), entry.getValue()));
        }

        if (filterList.isEmpty()) {
            return null;
        }

        if (filterList.size() == 1) {
            return filterList.get(0);
        }

        // Combinar dos filtros con AND
        if (filterList.size() == 2) {
            return new And(filterList.get(0), filterList.get(1));
        }

        // Combinar múltiples filtros con AND anidado
        Filter result = new And(filterList.get(0), filterList.get(1));
        for (int i = 2; i < filterList.size(); i++) {
            result = new And(result, filterList.get(i));
        }
        return result;
    }

    @Override
    @WithSpan("QdrantVectorStoreAdapter.upsertHistoricalEntryChunk")
    @LogExecutionTime(value = LogExecutionTime.LogLevel.DEBUG, logParameters = true)
    public Uni<Void> upsertHistoricalEntryChunk(HistoricalEntryChunk chunk) {
        log.debug("Upserting historical entry chunk for journal entry ID: {}", chunk.getJournalEntryId());

        return Uni.createFrom().item(() -> {
            // Metadata con chunk_type para diferenciación
            Map<String, Object> metadataMap = new HashMap<>();
            metadataMap.put("chunk_type", "journal_entry");  // KEY DIFERENCIADOR
            metadataMap.put("journal_entry_id", chunk.getJournalEntryId());
            metadataMap.put("entry_date", chunk.getEntryDate().toString());
            metadataMap.put("book_type", chunk.getBookType().name());
            metadataMap.put("description", chunk.getDescription());
            metadataMap.put("total_debit", chunk.getTotalDebit().toString());
            metadataMap.put("total_credit", chunk.getTotalCredit().toString());

            dev.langchain4j.data.document.Metadata metadata =
                    dev.langchain4j.data.document.Metadata.from(metadataMap);

            TextSegment textSegment = TextSegment.from(chunk.getChunkText(), metadata);
            dev.langchain4j.data.embedding.Embedding embedding =
                    dev.langchain4j.data.embedding.Embedding.from(chunk.getEmbedding());

            // Generar UUID determinístico para journal entry
            String namespace = "journal-entry-";
            UUID pointUuid = UUID.nameUUIDFromBytes((namespace + chunk.getJournalEntryId()).getBytes(StandardCharsets.UTF_8));
            String pointId = pointUuid.toString();
            embeddingStore.add(pointId, embedding);

            log.debug("Successfully upserted historical entry chunk");
            return (Void) null;
        }).runSubscriptionOn(io.smallrye.mutiny.infrastructure.Infrastructure.getDefaultWorkerPool());
    }

    @Override
    @WithSpan("QdrantVectorStoreAdapter.deleteHistoricalEntryChunk")
    @LogExecutionTime(value = LogExecutionTime.LogLevel.DEBUG, logParameters = true)
    public Uni<Void> deleteHistoricalEntryChunk(Integer journalEntryId) {
        log.debug("Deleting historical entry chunk for journal entry ID: {}", journalEntryId);

        return Uni.createFrom().item(() -> {
            // Regenerar el mismo UUID determinístico
            String namespace = "journal-entry-";
            UUID pointUuid = UUID.nameUUIDFromBytes((namespace + journalEntryId).getBytes(StandardCharsets.UTF_8));
            String pointId = pointUuid.toString();
            embeddingStore.remove(pointId);
            log.debug("Successfully deleted historical entry chunk for journal entry ID: {}", journalEntryId);
            return (Void) null;
        }).runSubscriptionOn(io.smallrye.mutiny.infrastructure.Infrastructure.getDefaultWorkerPool());
    }

    @Override
    @WithSpan("QdrantVectorStoreAdapter.searchSimilarHistoricalEntries")
    @LogExecutionTime(value = LogExecutionTime.LogLevel.DEBUG, logParameters = false)
    public Uni<List<HistoricalEntryChunk>> searchSimilarHistoricalEntries(float[] queryEmbedding, int limit) {
        log.debug("Searching similar historical entries, limit: {}", limit);

        return Uni.createFrom().item(() -> {
            dev.langchain4j.data.embedding.Embedding embedding =
                    dev.langchain4j.data.embedding.Embedding.from(queryEmbedding);

            // Filtrar solo journal entries
            Filter filter = new IsEqualTo("chunk_type", "journal_entry");

            EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                    .queryEmbedding(embedding)
                    .maxResults(limit)
                    .filter(filter)
                    .build();

            List<EmbeddingMatch<TextSegment>> matches =
                    embeddingStore.search(searchRequest).matches();

            List<HistoricalEntryChunk> results = matches.stream()
                    .map(this::toHistoricalEntryChunk)
                    .collect(Collectors.toList());

            log.debug("Found {} similar historical entries", results.size());
            return results;
        }).runSubscriptionOn(io.smallrye.mutiny.infrastructure.Infrastructure.getDefaultWorkerPool());
    }

    @Override
    @WithSpan("QdrantVectorStoreAdapter.searchHybrid")
    @LogExecutionTime(value = LogExecutionTime.LogLevel.DEBUG, logParameters = false)
    public Uni<HybridSearchResult> searchHybrid(float[] queryEmbedding, int accountLimit, int historicalLimit) {
        log.debug("Performing hybrid search (accounts: {}, historical: {})", accountLimit, historicalLimit);

        Uni<List<AccountSearchResult>> accountsUni = searchAccountsOnly(queryEmbedding, accountLimit);
        Uni<List<HistoricalEntryChunk>> entriesUni = searchSimilarHistoricalEntries(queryEmbedding, historicalLimit);

        return Uni.combine().all().unis(accountsUni, entriesUni)
                .asTuple()
                .map(tuple -> {
                    log.debug("Hybrid search completed: {} accounts, {} historical entries",
                            tuple.getItem1().size(), tuple.getItem2().size());
                    return HybridSearchResult.builder()
                            .accounts(tuple.getItem1())
                            .historicalEntries(tuple.getItem2())
                            .build();
                });
    }

    /**
     * Busca solo cuentas contables (filtra por chunk_type="account").
     */
    private Uni<List<AccountSearchResult>> searchAccountsOnly(float[] queryEmbedding, int limit) {
        log.debug("Searching accounts only, limit: {}", limit);

        return Uni.createFrom().item(() -> {
            dev.langchain4j.data.embedding.Embedding embedding =
                    dev.langchain4j.data.embedding.Embedding.from(queryEmbedding);

            // Filtrar solo cuentas
            Filter filter = new IsEqualTo("chunk_type", "account");

            EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                    .queryEmbedding(embedding)
                    .maxResults(limit)
                    .filter(filter)
                    .build();

            List<EmbeddingMatch<TextSegment>> matches =
                    embeddingStore.search(searchRequest).matches();

            List<AccountSearchResult> results = matches.stream()
                    .map(this::toSearchResult)
                    .collect(Collectors.toList());

            log.debug("Found {} accounts", results.size());
            return results;
        }).runSubscriptionOn(io.smallrye.mutiny.infrastructure.Infrastructure.getDefaultWorkerPool());
    }

    /**
     * Convierte un EmbeddingMatch a HistoricalEntryChunk.
     */
    private HistoricalEntryChunk toHistoricalEntryChunk(EmbeddingMatch<TextSegment> match) {
        TextSegment segment = match.embedded();
        Map<String, Object> metadata = segment.metadata().toMap();

        return HistoricalEntryChunk.builder()
                .journalEntryId(Integer.valueOf(metadata.get("journal_entry_id").toString()))
                .entryDate(LocalDate.parse((String) metadata.get("entry_date")))
                .description((String) metadata.get("description"))
                .bookType(AccountingBookType.valueOf((String) metadata.get("book_type")))
                .chunkText(segment.text())
                .totalDebit(new BigDecimal((String) metadata.get("total_debit")))
                .totalCredit(new BigDecimal((String) metadata.get("total_credit")))
                .similarityScore(match.score().floatValue())
                .build();
    }
}
