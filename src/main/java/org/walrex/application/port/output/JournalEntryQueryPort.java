package org.walrex.application.port.output;

import io.smallrye.mutiny.Uni;
import org.walrex.application.dto.query.JournalEntryFilter;
import org.walrex.application.dto.query.PageRequest;
import org.walrex.domain.model.JournalEntry;
import org.walrex.domain.model.PagedResult;

import java.util.List;
import java.util.Optional;

public interface JournalEntryQueryPort {

    /**
     * Finds an active journal entry by its ID.
     *
     * SQL: SELECT * FROM journal_entries WHERE id = $1 AND deleted_at IS NULL
     * SQL: SELECT * FROM journal_entry_lines WHERE journal_entry_id = $1
     *
     * @param id Unique identifier
     * @return Uni with Optional of the journal entry (empty if doesn't exist or is deleted)
     */
    Uni<Optional<JournalEntry>> findById(Integer id);

    /**
     * Finds a journal entry by ID including deleted ones.
     *
     * SQL: SELECT * FROM journal_entries WHERE id = $1
     * SQL: SELECT * FROM journal_entry_lines WHERE journal_entry_id = $1
     *
     * @param id Unique identifier
     * @return Uni with Optional of the journal entry
     */
    Uni<Optional<JournalEntry>> findByIdIncludingDeleted(Integer id);

    // ==================== Existence Checks ====================

    /**
     * Checks if a journal entry exists by ID.
     *
     * SQL: SELECT EXISTS(SELECT 1 FROM journal_entries WHERE id = $1 AND deleted_at IS NULL)
     *
     * @param id Identifier to check
     * @return Uni<Boolean> true if exists
     */
    Uni<Boolean> existsById(Integer id);

    // ==================== Lists with Pagination ====================

    /**
     * Lists journal entries with pagination and filters.
     *
     * Dynamic SQL with:
     * - WHERE conditions based on JournalEntryFilter
     * - ORDER BY based on PageRequest.sortBy
     * - LIMIT $n OFFSET $m for pagination
     *
     * @param pageRequest Pagination configuration
     * @param filter Optional filters
     * @return Uni with paginated result including metadata
     */
    Uni<PagedResult<JournalEntry>> findAll(PageRequest pageRequest, JournalEntryFilter filter);

    /**
     * Counts the total of journal entries that match the filters.
     *
     * SQL: SELECT COUNT(*) FROM journal_entries WHERE [conditions]
     *
     * @param filter Optional filters
     * @return Uni with the total count
     */
    Uni<Long> count(JournalEntryFilter filter);

    // ==================== Lists without Pagination ====================

    /**
     * Lists all journal entries that match the filter without pagination.
     *
     * Dynamic SQL with:
     * - WHERE conditions based on JournalEntryFilter
     * - ORDER BY entry_date DESC, id DESC (by default)
     *
     * @param filter Optional filters
     * @return Uni with complete list of journal entries
     */
    Uni<List<JournalEntry>> findAllWithFilter(JournalEntryFilter filter);

    // ==================== Special Queries ====================

    /**
     * Lists all deleted journal entries (for possible restoration).
     *
     * SQL: SELECT * FROM journal_entries WHERE deleted_at IS NOT NULL
     *
     * @return Uni with list of deleted journal entries
     */
    Uni<List<JournalEntry>> findAllDeleted();

    /**
     * Gets the next available book correlative for a specific book type.
     *
     * SQL: SELECT COALESCE(MAX(book_correlative), 0) + 1 FROM journal_entries
     *      WHERE book_type = $1 AND EXTRACT(YEAR FROM entry_date) = $2
     *
     * @param bookType Type of accounting book (DIARIO, VENTAS, COMPRAS)
     * @param year Year for the correlative
     * @return Uni with the next correlative number
     */
    Uni<Integer> getNextBookCorrelative(String bookType, Integer year);

    /**
     * Gets the next available operation number.
     *
     * SQL: SELECT COALESCE(MAX(operation_number), 0) + 1 FROM journal_entries
     *      WHERE EXTRACT(YEAR FROM entry_date) = $1
     *
     * @param year Year for the operation number
     * @return Uni with the next operation number
     */
    Uni<Integer> getNextOperationNumber(Integer year);
}
