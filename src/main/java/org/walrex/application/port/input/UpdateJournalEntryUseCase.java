package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.JournalEntry;

public interface UpdateJournalEntryUseCase {
    /**
     * Updates an existing journal entry.
     *
     * @param id ID of the journal entry to update
     * @param journalEntry Updated journal entry data
     * @return Uni with the updated journal entry
     * @throws org.walrex.domain.exception.JournalEntryNotFoundException
     *         if no journal entry exists with the provided ID
     * @throws org.walrex.domain.exception.UnbalancedJournalEntryException
     *         if total debits != total credits
     * @throws org.walrex.domain.exception.InvalidJournalEntryException
     *         if the entry doesn't meet business rules
     */
    Uni<JournalEntry> execute(Integer id, JournalEntry journalEntry);
}
