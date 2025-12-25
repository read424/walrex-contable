package org.walrex.infrastructure.adapter.outbound.persistence.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.JournalEntryEntity;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.JournalEntryLineEntity;
import org.walrex.infrastructure.adapter.outbound.persistence.mapper.JournalEntryMapper;

/**
 * Repository for JournalEntry persistence operations.
 *
 * Uses Panache Repository pattern which provides:
 * - Basic CRUD operations
 * - Typed queries
 * - Integrated pagination
 */
@Slf4j
@ApplicationScoped
public class JournalEntryRepository implements PanacheRepositoryBase<JournalEntryEntity, Integer> {

    @Inject
    JournalEntryMapper journalEntryMapper;

    /**
     * Persists a new journal entry with its lines.
     * Uses cascading to save lines automatically.
     *
     * @param entity JournalEntry entity to persist
     * @return Uni with the persisted entity (includes generated ID and timestamps)
     */
    public Uni<JournalEntryEntity> save(JournalEntryEntity entity) {
        log.debug("Persisting journal entry with {} lines", entity.getLines().size());

        // Set bidirectional relationship for all lines
        if (entity.getLines() != null) {
            for (JournalEntryLineEntity line : entity.getLines()) {
                line.setJournalEntry(entity);
            }
        }

        // Persist the entity (cascade will save lines automatically)
        return persist(entity)
                .invoke(saved -> log.debug("Journal entry persisted with id: {}", saved.getId()));
    }
}
