package org.walrex.application.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO for creating a new journal entry.
 *
 * Format validations are done here.
 * Business validations (balance, minimum lines, account existence) are done in the Service.
 */
public record CreateJournalEntryRequest(
    /**
     * Date of the journal entry.
     */
    @NotNull(message = "Entry date is required")
    LocalDate entryDate,

    /**
     * Type of accounting book: DIARIO, VENTAS, or COMPRAS.
     */
    @NotBlank(message = "Book type is required")
    String bookType,

    /**
     * General description/gloss for the journal entry.
     */
    @NotBlank(message = "Description is required")
    @Size(min = 3, max = 1000, message = "Description must be between 3 and 1000 characters")
    String description,

    /**
     * List of journal entry lines (details).
     * Must have at least 2 lines for a valid entry.
     */
    @NotNull(message = "Lines are required")
    @Size(min = 2, message = "At least 2 lines are required for a journal entry")
    List<@Valid JournalEntryLineRequest> lines
) {
    /**
     * Compact constructor for normalization.
     */
    public CreateJournalEntryRequest {
        if (bookType != null) {
            bookType = bookType.toUpperCase().trim();
        }
        if (description != null) {
            description = description.trim();
        }
    }
}
