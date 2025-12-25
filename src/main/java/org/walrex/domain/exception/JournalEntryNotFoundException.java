package org.walrex.domain.exception;

import lombok.Getter;

/**
 * Exception thrown when a journal entry is not found.
 * Translates to HTTP 404 Not Found.
 */
@Getter
public class JournalEntryNotFoundException extends RuntimeException {

    private final Integer journalEntryId;

    public JournalEntryNotFoundException(Integer id) {
        super("Journal entry not found with id: " + id);
        this.journalEntryId = id;
    }

    public JournalEntryNotFoundException(String message) {
        super(message);
        this.journalEntryId = null;
    }
}
