package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.JournalEntry;

public interface GetJournalEntryUseCase {
    /**
     * Gets a journal entry by its ID.
     *
     * @param id Unique identifier of the journal entry
     * @return Uni with the found journal entry
     * @throws org.walrex.domain.exception.JournalEntryNotFoundException
     *         if no journal entry exists with the provided ID
     */
    Uni<JournalEntry> findById(Integer id);
}
