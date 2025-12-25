package org.walrex.application.port.output;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.JournalEntry;

public interface JournalEntryRepositoryPort {

    /**
     * Persists a new journal entry with its lines.
     *
     * Expected SQL:
     * INSERT INTO journal_entries (...) VALUES ($1, $2, ...) RETURNING *
     * INSERT INTO journal_entry_lines (...) VALUES ($1, $2, ...) RETURNING *
     *
     * @param journalEntry Domain entity to persist
     * @return Uni with the persisted journal entry (includes timestamps from server)
     */
    Uni<JournalEntry> save(JournalEntry journalEntry);

    /**
     * Updates an existing journal entry and its lines.
     *
     * Expected SQL:
     * UPDATE journal_entries SET entry_date=$1, ... , updated_at=NOW()
     * WHERE id=$n AND deleted_at IS NULL RETURNING *
     * DELETE FROM journal_entry_lines WHERE journal_entry_id=$1
     * INSERT INTO journal_entry_lines (...) VALUES ...
     *
     * @param journalEntry Domain entity with updated data
     * @return Uni with the updated journal entry
     */
    Uni<JournalEntry> update(JournalEntry journalEntry);

    /**
     * Soft deletes a journal entry (sets deletedAt timestamp).
     *
     * Expected SQL:
     * UPDATE journal_entries SET deleted_at=NOW(), updated_at=NOW()
     * WHERE id=$1 AND deleted_at IS NULL
     *
     * @param id Identifier of the journal entry
     * @return Uni<Boolean> true if deleted (rowCount > 0), false if didn't exist
     */
    Uni<Boolean> softDelete(Integer id);

    /**
     * Hard deletes a journal entry and its lines.
     *
     * Expected SQL:
     * DELETE FROM journal_entry_lines WHERE journal_entry_id=$1
     * DELETE FROM journal_entries WHERE id=$1
     *
     * ⚠️ Use with caution. Prefer softDelete.
     *
     * @param id Identifier of the journal entry
     * @return Uni<Boolean> true if deleted, false if didn't exist
     */
    Uni<Boolean> hardDelete(Integer id);

    /**
     * Restores a previously deleted journal entry.
     *
     * Expected SQL:
     * UPDATE journal_entries SET deleted_at=NULL, updated_at=NOW()
     * WHERE id=$1 AND deleted_at IS NOT NULL
     *
     * @param id Identifier of the journal entry
     * @return Uni<Boolean> true if restored, false if didn't exist or wasn't deleted
     */
    Uni<Boolean> restore(Integer id);

    /**
     * Voids a journal entry (sets status to VOIDED).
     *
     * Expected SQL:
     * UPDATE journal_entries SET status='VOIDED', updated_at=NOW()
     * WHERE id=$1 AND deleted_at IS NULL
     *
     * @param id Identifier of the journal entry
     * @return Uni<Boolean> true if voided, false if didn't exist
     */
    Uni<Boolean> voidEntry(Integer id);
}
