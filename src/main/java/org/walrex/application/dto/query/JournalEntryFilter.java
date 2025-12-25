package org.walrex.application.dto.query;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

/**
 * Filter object for journal entry queries.
 *
 * Following the hexagonal pattern, this DTO belongs to the application layer
 * and is independent of the persistence implementation.
 */
@Data
@Builder
public class JournalEntryFilter {

    /**
     * General search (searches in description, doc serie, doc number).
     */
    private String search;

    /**
     * Filter by accounting book type (DIARIO, VENTAS, COMPRAS).
     */
    private String bookType;

    /**
     * Filter by status (ACTIVE, VOIDED).
     */
    private String status;

    /**
     * Filter by document type ID.
     */
    private Integer docTypeId;

    /**
     * Filter by entry date from (inclusive).
     */
    private LocalDate dateFrom;

    /**
     * Filter by entry date to (inclusive).
     */
    private LocalDate dateTo;

    /**
     * Filter by year (extracted from entry_date).
     */
    private Integer year;

    /**
     * Filter by month (extracted from entry_date).
     */
    private Integer month;

    /**
     * Include soft deleted entries.
     * By default "0" (false) - don't include deleted.
     */
    @Builder.Default
    private String includeDeleted = "0";
}
