package org.walrex.domain.model;

/**
 * Enum representing the status of a journal entry.
 */
public enum EntryStatus {
    /**
     * Active journal entry.
     */
    ACTIVE,

    /**
     * Voided/Cancelled journal entry (Anulado).
     */
    VOIDED;

    /**
     * Creates an EntryStatus from a string value.
     *
     * @param value the string value
     * @return the corresponding EntryStatus
     * @throws IllegalArgumentException if the value is invalid
     */
    public static EntryStatus fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Entry status cannot be null");
        }

        try {
            return EntryStatus.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                "Invalid entry status: " + value + ". Valid values are: ACTIVE, VOIDED"
            );
        }
    }
}
