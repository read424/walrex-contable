package org.walrex.domain.model;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Domain model representing a journal entry (asiento contable).
 * A journal entry is composed of multiple lines that must balance
 * (total debits must equal total credits).
 */
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode
public class JournalEntry {

    /**
     * Unique identifier for the journal entry.
     */
    private Integer id;

    /**
     * Date of the journal entry.
     */
    private LocalDate entryDate;

    /**
     * General description/gloss for the journal entry.
     */
    private String description;

    /**
     * General operation number (correlative).
     */
    private Integer operationNumber;

    /**
     * Correlative number per book type (DIARIO, VENTAS, COMPRAS).
     */
    private Integer bookCorrelative;

    /**
     * Type of accounting book.
     */
    private AccountingBookType bookType;

    /**
     * Status of the journal entry (ACTIVE or VOIDED).
     */
    private EntryStatus status;

    /**
     * List of journal entry lines (details).
     */
    private List<JournalEntryLine> lines;

    /**
     * Timestamp when the entry was created.
     */
    private OffsetDateTime createdAt;

    /**
     * Timestamp when the entry was last updated.
     */
    private OffsetDateTime updatedAt;

    /**
     * Timestamp when the entry was soft deleted.
     */
    private OffsetDateTime deletedAt;

    /**
     * Checks if the journal entry is deleted (soft delete).
     *
     * @return true if deleted
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * Checks if the journal entry is active and not deleted.
     *
     * @return true if active and not deleted
     */
    public boolean isActive() {
        return status == EntryStatus.ACTIVE && !isDeleted();
    }

    /**
     * Checks if the journal entry is voided.
     *
     * @return true if voided
     */
    public boolean isVoided() {
        return status == EntryStatus.VOIDED;
    }

    /**
     * Calculates the total debit amount from all lines.
     *
     * @return total debit amount
     */
    public BigDecimal getTotalDebit() {
        if (lines == null || lines.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return lines.stream()
            .map(line -> line.getDebit() != null ? line.getDebit() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calculates the total credit amount from all lines.
     *
     * @return total credit amount
     */
    public BigDecimal getTotalCredit() {
        if (lines == null || lines.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return lines.stream()
            .map(line -> line.getCredit() != null ? line.getCredit() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Checks if the journal entry is balanced (debits == credits).
     *
     * @return true if balanced
     */
    public boolean isBalanced() {
        return getTotalDebit().compareTo(getTotalCredit()) == 0;
    }

    /**
     * Gets the difference between total debits and credits.
     *
     * @return difference amount (should be zero for balanced entries)
     */
    public BigDecimal getBalanceDifference() {
        return getTotalDebit().subtract(getTotalCredit());
    }

    /**
     * Validates that the journal entry has at least the minimum required lines.
     *
     * @return true if has at least 2 lines
     */
    public boolean hasMinimumLines() {
        return lines != null && lines.size() >= 2;
    }
}
