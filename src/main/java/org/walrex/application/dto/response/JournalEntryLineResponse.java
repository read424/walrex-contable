package org.walrex.application.dto.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * Response DTO for a journal entry line.
 */
public record JournalEntryLineResponse(
    Integer id,
    Integer journalEntryId,
    Integer accountId,
    BigDecimal debit,
    BigDecimal credit,
    String description,
    List<JournalEntryDocumentResponse> documents
) {
}
