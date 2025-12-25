package org.walrex.application.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Response DTO for a journal entry.
 *
 * Note: We use record (immutable) for DTOs.
 * We don't expose deletedAt to the outside for security.
 */
public record JournalEntryResponse(
    Integer id,
    LocalDate entryDate,
    String description,
    String reference,
    Integer docTypeId,
    String docSerie,
    String docNumber,
    Integer operationNumber,
    Integer bookCorrelative,
    String bookType,
    String status,
    List<JournalEntryLineResponse> lines,
    BigDecimal totalDebit,
    BigDecimal totalCredit,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}
