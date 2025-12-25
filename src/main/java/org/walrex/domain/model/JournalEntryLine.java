package org.walrex.domain.model;

import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Domain model representing a line/detail of a journal entry.
 * Each line represents a debit or credit to an account.
 */
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode
public class JournalEntryLine {

    /**
     * Unique identifier for the journal entry line.
     */
    private Integer id;

    /**
     * Foreign key to the parent journal entry.
     */
    private Integer journalEntryId;

    /**
     * Foreign key to the account (from accounts table).
     */
    private Integer accountId;

    /**
     * Debit amount. Must be >= 0.
     */
    private BigDecimal debit;

    /**
     * Credit amount. Must be >= 0.
     */
    private BigDecimal credit;

    /**
     * Description for this specific line.
     * Can be different from the main journal entry description.
     */
    private String description;

    /**
     * List of documents attached to this line.
     */
    @Builder.Default
    private List<JournalEntryDocument> documents = new ArrayList<>();

    /**
     * Validates that the line has valid amounts.
     * At least one of debit or credit must be greater than zero.
     *
     * @return true if the line is valid
     */
    public boolean isValid() {
        return (debit != null && debit.compareTo(BigDecimal.ZERO) >= 0) &&
               (credit != null && credit.compareTo(BigDecimal.ZERO) >= 0) &&
               (debit.compareTo(BigDecimal.ZERO) > 0 || credit.compareTo(BigDecimal.ZERO) > 0);
    }

    /**
     * Returns the net amount (debit - credit) for this line.
     *
     * @return the net amount
     */
    public BigDecimal getNetAmount() {
        BigDecimal debitAmount = debit != null ? debit : BigDecimal.ZERO;
        BigDecimal creditAmount = credit != null ? credit : BigDecimal.ZERO;
        return debitAmount.subtract(creditAmount);
    }
}
