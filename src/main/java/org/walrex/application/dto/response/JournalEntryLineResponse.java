package org.walrex.application.dto.response;

import java.math.BigDecimal;

/**
 * Response DTO for a journal entry line.
 */
public record JournalEntryLineResponse(
    Integer id,
    Integer journalEntryId,
    Integer accountId,
    BigDecimal debit,
    BigDecimal credit,
    String description
) {
}
