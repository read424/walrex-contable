package org.walrex.infrastructure.adapter.inbound.rest.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.walrex.application.dto.request.DocumentUploadRequest;
import org.walrex.application.port.output.FileStoragePort;
import org.walrex.domain.model.JournalEntryDocument;

import java.io.ByteArrayInputStream;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Service for processing document uploads.
 * Decodes base64 files and stores them in filesystem.
 */
@Slf4j
@ApplicationScoped
public class DocumentProcessorService {

    @Inject
    FileStoragePort fileStoragePort;

    /**
     * Processes a single document upload.
     * Decodes base64, stores file, and creates JournalEntryDocument with metadata.
     *
     * @param document Document upload request with base64 content
     * @return Uni with JournalEntryDocument containing file metadata
     */
    public Uni<JournalEntryDocument> processDocument(DocumentUploadRequest document) {
        log.debug("Processing document: {}", document.name());

        return Uni.createFrom().item(() -> {
                    // Decode base64
                    byte[] fileBytes = Base64.getDecoder().decode(document.base64());
                    return new ByteArrayInputStream(fileBytes);
                })
                // Store file in filesystem
                .onItem().transformToUni(inputStream ->
                        fileStoragePort.store(inputStream, document.name(), document.type(), document.size())
                )
                // Create JournalEntryDocument with metadata
                .onItem().transform(storedInfo -> JournalEntryDocument.builder()
                        .originalFilename(document.name())
                        .storedFilename(storedInfo.storedFilename())
                        .filePath(storedInfo.filePath())
                        .mimeType(document.type())
                        .fileSize(document.size())
                        .uploadedAt(OffsetDateTime.now(ZoneOffset.UTC))
                        .build()
                )
                .invoke(doc -> log.info("Document processed successfully: {} -> {}",
                        doc.getOriginalFilename(), doc.getStoredFilename()));
    }

    /**
     * Processes multiple document uploads in parallel.
     *
     * @param documents List of document upload requests
     * @return Uni with list of JournalEntryDocuments
     */
    public Uni<List<JournalEntryDocument>> processDocuments(List<DocumentUploadRequest> documents) {
        if (documents == null || documents.isEmpty()) {
            return Uni.createFrom().item(new ArrayList<>());
        }

        log.debug("Processing {} documents", documents.size());

        // Process all documents in parallel
        List<Uni<JournalEntryDocument>> documentUnis = documents.stream()
                .map(this::processDocument)
                .toList();

        return Uni.combine().all().unis(documentUnis).with(list -> {
            List<JournalEntryDocument> result = new ArrayList<>();
            for (Object obj : list) {
                result.add((JournalEntryDocument) obj);
            }
            return result;
        });
    }
}
