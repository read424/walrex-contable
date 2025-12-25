package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;

public interface DeleteJournalEntryUseCase {
    /**
     * Soft deletes a journal entry (sets deletedAt timestamp).
     *
     * @param id ID of the journal entry to delete
     * @return Uni with true if deleted successfully
     * @throws org.walrex.domain.exception.JournalEntryNotFoundException
     *         if no journal entry exists with the provided ID
     */
    Uni<Boolean> execute(Integer id);

    /**
     * Restores a soft-deleted journal entry.
     *
     * @param id ID of the journal entry to restore
     * @return Uni with true if restored successfully
     * @throws org.walrex.domain.exception.JournalEntryNotFoundException
     *         if no journal entry exists with the provided ID
     */
    Uni<Boolean> restore(Integer id);

    /**
     * Voids a journal entry (sets status to VOIDED).
     * This is the accounting way to "cancel" an entry without deleting it.
     *
     * @param id ID of the journal entry to void
     * @return Uni with true if voided successfully
     * @throws org.walrex.domain.exception.JournalEntryNotFoundException
     *         if no journal entry exists with the provided ID
     */
    Uni<Boolean> voidEntry(Integer id);
}
