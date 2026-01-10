package org.walrex.domain.service;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.walrex.application.port.input.SyncHistoricalEntriesUseCase;
import org.walrex.application.port.output.EmbeddingCachePort;
import org.walrex.application.port.output.JournalEntryQueryPort;
import org.walrex.application.port.output.VectorStorePort;
import org.walrex.domain.model.AccountingBookType;
import org.walrex.domain.model.JournalEntry;
import org.walrex.domain.model.JournalEntryDocument;
import org.walrex.domain.model.JournalEntryLine;
import org.walrex.infrastructure.adapter.logging.LogExecutionTime;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Servicio para sincronizar asientos contables históricos a Qdrant.
 * Esto permite que el RAG aprenda de asientos previos.
 */
@Slf4j
@ApplicationScoped
public class HistoricalEntriesSyncService implements SyncHistoricalEntriesUseCase {

    @Inject
    JournalEntryQueryPort journalEntryQueryPort;

    @Inject
    JournalEntryChunkingService chunkingService;

    @Inject
    VectorStorePort vectorStorePort;

    @Inject
    EmbeddingCachePort embeddingCache;

    @Inject
    HashService hashService;

    @ConfigProperty(name = "rag.historical.auto-sync-enabled", defaultValue = "true")
    Boolean autoSyncEnabled;

    @Override
    @WithSpan("HistoricalEntriesSyncService.syncEntry")
    @LogExecutionTime(value = LogExecutionTime.LogLevel.INFO, logParameters = true)
    public Uni<Void> syncEntry(Integer journalEntryId) {
        if (!autoSyncEnabled) {
            log.debug("Historical entries auto-sync is disabled, skipping entry {}", journalEntryId);
            return Uni.createFrom().voidItem();
        }

        log.info("Syncing journal entry {} to Qdrant with automatic cache detection", journalEntryId);

        return journalEntryQueryPort.findById(journalEntryId)
                .onItem().transformToUni(optionalEntry -> {
                    if (optionalEntry.isEmpty()) {
                        return Uni.createFrom().failure(
                                new IllegalArgumentException("Journal entry not found: " + journalEntryId)
                        );
                    }

                    JournalEntry entry = optionalEntry.get();

                    // Buscar documentos adjuntos en las líneas del asiento
                    String imageHash = extractImageHashFromEntry(entry);

                    if (imageHash != null) {
                        log.info("Found document attachment in entry {}. Trying to reuse cached embedding with hash: {}",
                                journalEntryId, imageHash);

                        // Intentar recuperar embedding del caché
                        return embeddingCache.get(imageHash, entry.getBookType())
                                .onItem().transformToUni(cachedEmbedding -> {
                                    if (cachedEmbedding != null) {
                                        log.info("✅ Embedding cache HIT! Reusing cached embedding for entry {}",
                                                journalEntryId);
                                        // REUTILIZAR embedding del caché
                                        return chunkingService.createHistoricalChunkFromCache(
                                                entry,
                                                cachedEmbedding.getEmbedding(),
                                                imageHash
                                        );
                                    } else {
                                        log.info("Embedding cache MISS for entry {}. Generating new embedding",
                                                journalEntryId);
                                        // Fallback: generar nuevo embedding pero guardar el hash
                                        return chunkingService.createHistoricalChunk(entry, imageHash);
                                    }
                                })
                                .chain(chunk -> vectorStorePort.upsertHistoricalEntryChunk(chunk));
                    } else {
                        log.debug("No document attachments found in entry {}. Generating embedding without cache",
                                journalEntryId);
                        // Sin documentos adjuntos, crear chunk normal
                        return chunkingService.createHistoricalChunk(entry, null)
                                .chain(chunk -> vectorStorePort.upsertHistoricalEntryChunk(chunk));
                    }
                })
                .onItem().invoke(() ->
                        log.info("Successfully synced journal entry {} to Qdrant", journalEntryId)
                )
                .onFailure().invoke(throwable ->
                        log.error("Failed to sync journal entry {} to Qdrant", journalEntryId, throwable)
                );
    }

    /**
     * Extrae el hash SHA-256 del primer documento adjunto encontrado en las líneas del asiento.
     * Si no hay documentos, retorna null.
     *
     * @param entry Asiento contable con posibles documentos adjuntos
     * @return Hash SHA-256 del primer documento, o null si no hay documentos
     */
    private String extractImageHashFromEntry(JournalEntry entry) {
        if (entry.getLines() == null || entry.getLines().isEmpty()) {
            return null;
        }

        // Buscar el primer documento adjunto en cualquier línea
        for (JournalEntryLine line : entry.getLines()) {
            if (line.getDocuments() != null && !line.getDocuments().isEmpty()) {
                JournalEntryDocument firstDoc = line.getDocuments().getFirst();

                try {
                    // Leer el archivo y generar hash
                    Path filePath = Path.of(firstDoc.getFilePath());

                    if (Files.exists(filePath)) {
                        byte[] fileBytes = Files.readAllBytes(filePath);
                        String hash = hashService.generateSHA256(fileBytes);

                        log.debug("Generated hash {} for document: {}", hash, firstDoc.getOriginalFilename());
                        return hash;
                    } else {
                        log.warn("Document file not found: {}", filePath);
                    }
                } catch (IOException e) {
                    log.error("Error reading document file: {}", firstDoc.getFilePath(), e);
                }
            }
        }

        return null;
    }


    @Override
    @WithSpan("HistoricalEntriesSyncService.removeEntry")
    @LogExecutionTime(value = LogExecutionTime.LogLevel.INFO, logParameters = true)
    public Uni<Void> removeEntry(Integer journalEntryId) {
        log.info("Removing journal entry {} from Qdrant", journalEntryId);
        return vectorStorePort.deleteHistoricalEntryChunk(journalEntryId)
                .onItem().invoke(() -> log.info("Successfully removed journal entry {} from Qdrant", journalEntryId))
                .onFailure().invoke(throwable -> log.error("Failed to remove journal entry {} from Qdrant", journalEntryId, throwable));
    }

    @Override
    @WithSpan("HistoricalEntriesSyncService.syncAllEntries")
    @LogExecutionTime(value = LogExecutionTime.LogLevel.INFO, logParameters = true)
    public Uni<Integer> syncAllEntries() {
        log.info("Starting sync of ALL historical journal entries to Qdrant");

        // Buscar todos los asientos (sin filtro)
        return journalEntryQueryPort.findAllWithFilter(null)
                .onItem().transformToUni(entries -> {
                    log.info("Found {} journal entries to sync", entries.size());

                    if (entries.isEmpty()) {
                        return Uni.createFrom().item(0);
                    }

                    // Sincronizar todos en paralelo
                    return Uni.combine().all().unis(
                            entries.stream()
                                    .map(entry -> syncSingleEntry(entry))
                                    .toList()
                    ).with(results -> results.size());
                })
                .onItem().invoke(count ->
                        log.info("Successfully synced {} historical journal entries to Qdrant", count)
                )
                .onFailure().invoke(throwable ->
                        log.error("Failed to sync all historical entries", throwable)
                );
    }

    /**
     * Sincroniza un asiento individual (helper method).
     */
    private Uni<Void> syncSingleEntry(JournalEntry entry) {
        return chunkingService.createHistoricalChunk(entry, null)
                .chain(chunk -> vectorStorePort.upsertHistoricalEntryChunk(chunk))
                .onItem().invoke(() ->
                        log.debug("Synced entry ID {} to Qdrant", entry.getId())
                )
                .onFailure().recoverWithItem(throwable -> {
                    log.warn("Failed to sync entry ID {}, continuing...", entry.getId(), throwable);
                    return null; // Continuar con los demás aunque uno falle
                });
    }
}
