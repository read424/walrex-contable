package org.walrex.application.dto.response;

import java.time.OffsetDateTime;

/**
 * Response DTO for a journal entry document.
 * Does not include the file content (base64), only metadata.
 */
public record JournalEntryDocumentResponse(
    Integer id,
    Integer journalEntryLineId,
    String originalFilename,
    String storedFilename,
    String mimeType,
    Long fileSize,
    OffsetDateTime uploadedAt
) {
}
