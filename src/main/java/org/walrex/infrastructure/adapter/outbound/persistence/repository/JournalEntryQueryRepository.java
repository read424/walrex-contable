package org.walrex.infrastructure.adapter.outbound.persistence.repository;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

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
}
