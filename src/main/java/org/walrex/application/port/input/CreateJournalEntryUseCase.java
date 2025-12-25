package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.JournalEntry;

public interface CreateJournalEntryUseCase {
    /**
     * Creates a new journal entry in the system.
     *
     * @param journalEntry Data needed to create the journal entry
     * @return Uni with the created journal entry
     * @throws org.walrex.domain.exception.UnbalancedJournalEntryException
     *         if total debits != total credits
     * @throws org.walrex.domain.exception.InvalidJournalEntryException
     *         if the entry doesn't meet business rules (minimum lines, valid accounts, etc.)
     */
    Uni<JournalEntry> execute(JournalEntry journalEntry);
}
