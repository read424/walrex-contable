package org.walrex.application.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for a journal entry line (detail).
 * Represents a single debit or credit to an account.
 */
public record JournalEntryLineRequest(
    /**
     * Account ID from the accounts table.
     */
    @NotNull(message = "Account ID is required")
    Integer accountId,

    /**
     * Debit amount. Must be >= 0.
     */
    @NotNull(message = "Debit amount is required")
    @DecimalMin(value = "0.0", message = "Debit must be greater than or equal to 0")
    BigDecimal debit,

    /**
     * Credit amount. Must be >= 0.
     */
    @NotNull(message = "Credit amount is required")
    @DecimalMin(value = "0.0", message = "Credit must be greater than or equal to 0")
    BigDecimal credit,

    /**
     * Optional description for this specific line.
     * If null, will use the main entry description.
     */
    @Size(max = 500, message = "Line description must not exceed 500 characters")
    String description,

    /**
     * Optional list of documents to attach to this line.
     * Documents are uploaded as base64-encoded strings.
     */
    @Valid
    List<DocumentUploadRequest> documents
) {
    /**
     * Compact constructor for normalization.
     */
    public JournalEntryLineRequest {
        if (description != null) {
            description = description.trim();
            if (description.isEmpty()) {
                description = null;
            }
        }
    }
}
