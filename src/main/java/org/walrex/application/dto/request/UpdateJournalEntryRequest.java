package org.walrex.application.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO for updating an existing journal entry.
 *
 * Uses the same validations as CreateJournalEntryRequest.
 * In a PUT, all fields are required (complete replacement).
 */
public record UpdateJournalEntryRequest(
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
     * Optional reference to internal documents.
     */
    @Size(max = 100, message = "Reference must not exceed 100 characters")
    String reference,

    /**
     * Optional document type ID (from document_types table).
     */
    Integer docTypeId,

    /**
     * Optional document series.
     */
    @Size(max = 10, message = "Document series must not exceed 10 characters")
    String docSerie,

    /**
     * Optional document number.
     */
    @Size(max = 20, message = "Document number must not exceed 20 characters")
    String docNumber,

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
    public UpdateJournalEntryRequest {
        if (bookType != null) {
            bookType = bookType.toUpperCase().trim();
        }
        if (description != null) {
            description = description.trim();
        }
        if (reference != null) {
            reference = reference.trim();
            if (reference.isEmpty()) {
                reference = null;
            }
        }
        if (docSerie != null) {
            docSerie = docSerie.trim();
            if (docSerie.isEmpty()) {
                docSerie = null;
            }
        }
        if (docNumber != null) {
            docNumber = docNumber.trim();
            if (docNumber.isEmpty()) {
                docNumber = null;
            }
        }
    }
}
