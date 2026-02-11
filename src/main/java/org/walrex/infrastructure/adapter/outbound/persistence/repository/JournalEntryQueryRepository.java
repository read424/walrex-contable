package org.walrex.infrastructure.adapter.outbound.persistence.repository;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.walrex.application.dto.query.JournalEntryFilter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository for JournalEntry query operations.
 * Handles read operations and correlative generation.
 */
@Slf4j
@ApplicationScoped
public class JournalEntryQueryRepository {

    /**
     * Gets the next available book correlative for a specific book type and year.
     * Book correlatives are sequential numbers per book type (DIARIO, VENTAS, COMPRAS) per year.
     *
     * @param bookType Type of accounting book (DIARIO, VENTAS, COMPRAS)
     * @param year Year for the correlative
     * @return Uni with the next correlative number
     */
    public Uni<Integer> getNextBookCorrelative(String bookType, Integer year) {
        log.debug("Getting next book correlative for bookType: {}, year: {}", bookType, year);

        String sql = """
            SELECT COALESCE(MAX(book_correlative), 0) + 1
            FROM journal_entries
            WHERE book_type = ?1
            AND EXTRACT(YEAR FROM entry_date) = ?2
            """;

        return Panache.getSession()
                .chain(session -> session.createNativeQuery(sql, Integer.class)
                        .setParameter(1, bookType)
                        .setParameter(2, year)
                        .getSingleResult())
                .invoke(correlative -> log.debug("Next book correlative for {} ({}): {}",
                        bookType, year, correlative));
    }

    /**
     * Gets the next available operation number for a specific year.
     * Operation numbers are global sequential numbers per year across all book types.
     *
     * @param year Year for the operation number
     * @return Uni with the next operation number
     */
    public Uni<Integer> getNextOperationNumber(Integer year) {
        log.debug("Getting next operation number for year: {}", year);

        String sql = """
            SELECT COALESCE(MAX(operation_number), 0) + 1
            FROM journal_entries
            WHERE EXTRACT(YEAR FROM entry_date) = ?1
            """;

        return Panache.getSession()
                .chain(session -> session.createNativeQuery(sql, Integer.class)
                        .setParameter(1, year)
                        .getSingleResult())
                .invoke(operationNumber -> log.debug("Next operation number for {}: {}",
                        year, operationNumber));
    }

    /**
     * Finds journal entries with pagination and filtering.
     *
     * @param filter Filter criteria
     * @param page Page number (0-indexed)
     * @param size Page size
     * @return Uni with list of JournalEntryEntity
     */
    /**
     * Finds journal entries with pagination using native SQL.
     * Returns complete data aggregated in JSON to avoid Hibernate EAGER issues.
     */
    public Uni<List<Object[]>> findEntriesWithDetailsNative(JournalEntryFilter filter, int page, int size) {
        log.debug("Finding journal entries with native SQL: page={}, size={}", page, size);

        StringBuilder sql = new StringBuilder("""
            SELECT
                je.id, je.entry_date, je.description, je.operation_number,
                je.book_correlative, je.book_type, je.status,
                je.created_at, je.updated_at,
                COALESCE(json_agg(
                    json_build_object(
                        'id', jel.id,
                        'accountId', jel.account_id,
                        'debit', jel.debit,
                        'credit', jel.credit,
                        'description', jel.description,
                        'documents', COALESCE((
                            SELECT json_agg(
                                json_build_object(
                                    'id', doc.id,
                                    'originalFilename', doc.original_filename,
                                    'storedFilename', doc.stored_filename,
                                    'filePath', doc.file_path,
                                    'mimeType', doc.mime_type,
                                    'fileSize', doc.file_size,
                                    'uploadedAt', doc.uploaded_at
                                )
                            )
                            FROM journal_entry_documents doc
                            WHERE doc.journal_entry_line_id = jel.id
                        ), '[]'::json)
                    ) ORDER BY jel.id
                ) FILTER (WHERE jel.id IS NOT NULL), '[]'::json) as lines
            FROM journal_entries je
            LEFT JOIN journal_entry_lines jel ON jel.journal_entry_id = je.id
            WHERE 1=1
            """);

        Map<String, Object> params = new HashMap<>();
        appendNativeFilters(sql, params, filter);

        sql.append("""
             GROUP BY je.id, je.entry_date, je.description, je.operation_number,
                      je.book_correlative, je.book_type, je.status,
                      je.created_at, je.updated_at
             ORDER BY je.entry_date DESC, je.id DESC
             LIMIT :limit OFFSET :offset
            """);

        params.put("limit", size);
        params.put("offset", page * size);

        return Panache.getSession()
                .chain(session -> {
                    var query = session.createNativeQuery(sql.toString(), Object[].class);
                    params.forEach(query::setParameter);
                    return query.getResultList();
                })
                .invoke(results -> log.debug("Found {} journal entries via native SQL", results.size()));
    }

    /**
     * Appends filter conditions to native SQL query.
     */
    private void appendNativeFilters(StringBuilder sql, Map<String, Object> params, JournalEntryFilter filter) {
        if (filter == null) {
            return;
        }

        // Filter by year
        if (filter.getYear() != null) {
            sql.append(" AND EXTRACT(YEAR FROM entry_date) = :year");
            params.put("year", filter.getYear());
        }

        // Filter by month
        if (filter.getMonth() != null) {
            sql.append(" AND EXTRACT(MONTH FROM entry_date) = :month");
            params.put("month", filter.getMonth());
        }

        // Filter by date range
        if (filter.getDateFrom() != null) {
            sql.append(" AND entry_date >= :dateFrom");
            params.put("dateFrom", java.sql.Date.valueOf(filter.getDateFrom()));
        }

        if (filter.getDateTo() != null) {
            sql.append(" AND entry_date <= :dateTo");
            params.put("dateTo", java.sql.Date.valueOf(filter.getDateTo()));
        }

        // Filter by book type
        if (filter.getBookType() != null && !filter.getBookType().isBlank()) {
            sql.append(" AND book_type = :bookType");
            params.put("bookType", filter.getBookType());
        }

        // Filter by status
        if (filter.getStatus() != null && !filter.getStatus().isBlank()) {
            sql.append(" AND status = :status");
            params.put("status", filter.getStatus());
        }

        // Filter by search (description)
        if (filter.getSearch() != null && !filter.getSearch().isBlank()) {
            sql.append(" AND LOWER(description) LIKE :search");
            params.put("search", "%" + filter.getSearch().toLowerCase() + "%");
        }

        // Exclude soft-deleted by default
        if (!"1".equals(filter.getIncludeDeleted())) {
            sql.append(" AND deleted_at IS NULL");
        }
    }

    /**
     * Counts journal entries with filtering.
     *
     * @param filter Filter criteria
     * @return Uni with total count
     */
    public Uni<Long> countAll(JournalEntryFilter filter) {
        log.debug("Counting journal entries with filter: {}", filter);

        StringBuilder hql = new StringBuilder("SELECT COUNT(e) FROM JournalEntryEntity e WHERE 1=1");
        Map<String, Object> params = new HashMap<>();

        appendFilters(hql, params, filter);

        return Panache.getSession()
                .chain(session -> {
                    var query = session.createQuery(hql.toString(), Long.class);
                    params.forEach(query::setParameter);
                    return query.getSingleResult();
                })
                .invoke(count -> log.debug("Total journal entries: {}", count));
    }

    /**
     * Appends filter conditions to HQL query.
     */
    private void appendFilters(StringBuilder hql, Map<String, Object> params, JournalEntryFilter filter) {
        if (filter == null) {
            return;
        }

        // Filter by year
        if (filter.getYear() != null) {
            hql.append(" AND EXTRACT(YEAR FROM e.entryDate) = :year");
            params.put("year", filter.getYear());
        }

        // Filter by month
        if (filter.getMonth() != null) {
            hql.append(" AND EXTRACT(MONTH FROM e.entryDate) = :month");
            params.put("month", filter.getMonth());
        }

        // Filter by date range
        if (filter.getDateFrom() != null) {
            hql.append(" AND e.entryDate >= :dateFrom");
            params.put("dateFrom", java.sql.Date.valueOf(filter.getDateFrom()));
        }

        if (filter.getDateTo() != null) {
            hql.append(" AND e.entryDate <= :dateTo");
            params.put("dateTo", java.sql.Date.valueOf(filter.getDateTo()));
        }

        // Filter by book type
        if (filter.getBookType() != null && !filter.getBookType().isBlank()) {
            hql.append(" AND e.bookType = :bookType");
            params.put("bookType", org.walrex.domain.model.AccountingBookType.valueOf(filter.getBookType()));
        }

        // Filter by status
        if (filter.getStatus() != null && !filter.getStatus().isBlank()) {
            hql.append(" AND e.status = :status");
            params.put("status", org.walrex.domain.model.EntryStatus.valueOf(filter.getStatus()));
        }

        // Filter by search (description)
        if (filter.getSearch() != null && !filter.getSearch().isBlank()) {
            hql.append(" AND LOWER(e.description) LIKE :search");
            params.put("search", "%" + filter.getSearch().toLowerCase() + "%");
        }

        // Exclude soft-deleted by default
        if (!"1".equals(filter.getIncludeDeleted())) {
            hql.append(" AND e.deletedAt IS NULL");
        }
    }
}
