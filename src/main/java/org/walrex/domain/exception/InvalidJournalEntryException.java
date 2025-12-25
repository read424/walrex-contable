package org.walrex.domain.exception;

import lombok.Getter;

/**
 * Exception thrown when a journal entry has invalid data.
 * Translates to HTTP 400 Bad Request.
 */
@Getter
public class InvalidJournalEntryException extends RuntimeException {

    private final String field;
    private final String reason;

    public InvalidJournalEntryException(String message) {
        super(message);
        this.field = null;
        this.reason = message;
    }

    public InvalidJournalEntryException(String field, String reason) {
        super(String.format("Invalid journal entry - %s: %s", field, reason));
        this.field = field;
        this.reason = reason;
    }
}
