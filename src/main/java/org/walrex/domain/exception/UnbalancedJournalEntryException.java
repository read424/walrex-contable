package org.walrex.domain.exception;

import lombok.Getter;

import java.math.BigDecimal;

/**
 * Exception thrown when a journal entry is not balanced
 * (total debits != total credits).
 * Translates to HTTP 400 Bad Request.
 */
@Getter
public class UnbalancedJournalEntryException extends RuntimeException {

    private final BigDecimal totalDebit;
    private final BigDecimal totalCredit;
    private final BigDecimal difference;

    public UnbalancedJournalEntryException(BigDecimal totalDebit, BigDecimal totalCredit) {
        super(String.format(
            "Journal entry is not balanced. Debits: %s, Credits: %s, Difference: %s",
            totalDebit,
            totalCredit,
            totalDebit.subtract(totalCredit)
        ));
        this.totalDebit = totalDebit;
        this.totalCredit = totalCredit;
        this.difference = totalDebit.subtract(totalCredit);
    }
}
