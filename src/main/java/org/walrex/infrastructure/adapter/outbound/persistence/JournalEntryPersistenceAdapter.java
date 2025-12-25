package org.walrex.infrastructure.adapter.outbound.persistence;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.walrex.application.port.output.JournalEntryQueryPort;
import org.walrex.application.port.output.JournalEntryRepositoryPort;
import org.walrex.domain.model.JournalEntry;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.JournalEntryDocumentEntity;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.JournalEntryEntity;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.JournalEntryLineEntity;
import org.walrex.infrastructure.adapter.outbound.persistence.mapper.JournalEntryMapper;
import org.walrex.infrastructure.adapter.outbound.persistence.repository.JournalEntryQueryRepository;
import org.walrex.infrastructure.adapter.outbound.persistence.repository.JournalEntryRepository;

import java.util.ArrayList;

/**
 * Persistence adapter that implements output ports for JournalEntry.
 *
 * Following the hexagonal pattern (Ports & Adapters), this adapter:
 * - Implements port interfaces (JournalEntryRepositoryPort, JournalEntryQueryPort)
 * - Translates between domain model (JournalEntry) and persistence layer (JournalEntryEntity)
 * - Uses mapper for transformations
 * - Delegates persistence operations to Panache repositories
 */
@Slf4j
@ApplicationScoped
public class JournalEntryPersistenceAdapter implements JournalEntryRepositoryPort, JournalEntryQueryPort {

    @Inject
    JournalEntryRepository repository;

    @Inject
    JournalEntryQueryRepository queryRepository;

    @Inject
    JournalEntryMapper mapper;

    // ==================== JournalEntryRepositoryPort - Write Operations ====================

    @Override
    public Uni<JournalEntry> save(JournalEntry journalEntry) {
        log.debug("Saving journal entry: {}", journalEntry.getDescription());

        // Convert domain model to entity
        JournalEntryEntity entity = mapper.toEntity(journalEntry);

        // Map lines and documents manually to ensure bidirectional relationships
        if (journalEntry.getLines() != null && !journalEntry.getLines().isEmpty()) {
            var lineEntities = new ArrayList<JournalEntryLineEntity>();

            journalEntry.getLines().forEach(line -> {
                // Map the line
                JournalEntryLineEntity lineEntity = mapper.lineToEntity(line);
                lineEntity.setJournalEntry(entity);

                // Map documents for this line
                if (line.getDocuments() != null && !line.getDocuments().isEmpty()) {
                    var documentEntities = new ArrayList<JournalEntryDocumentEntity>();

                    line.getDocuments().forEach(document -> {
                        JournalEntryDocumentEntity docEntity = mapper.documentToEntity(document);
                        docEntity.setJournalEntryLine(lineEntity);
                        documentEntities.add(docEntity);
                    });

                    lineEntity.setDocuments(documentEntities);
                }

                lineEntities.add(lineEntity);
            });

            entity.setLines(lineEntities);
        }

        // Persist entity with cascading
        return repository.save(entity)
                .onItem().transform(savedEntity -> {
                    log.debug("Journal entry saved with id: {}", savedEntity.getId());
                    return mapper.toDomain(savedEntity);
                });
    }

    @Override
    public Uni<JournalEntry> update(JournalEntry journalEntry) {
        // TODO: Implement when needed
        return Uni.createFrom().failure(
                new UnsupportedOperationException("Update not implemented yet")
        );
    }

    @Override
    public Uni<Boolean> softDelete(Integer id) {
        // TODO: Implement when needed
        return Uni.createFrom().failure(
                new UnsupportedOperationException("Soft delete not implemented yet")
        );
    }

    @Override
    public Uni<Boolean> hardDelete(Integer id) {
        // TODO: Implement when needed
        return Uni.createFrom().failure(
                new UnsupportedOperationException("Hard delete not implemented yet")
        );
    }

    @Override
    public Uni<Boolean> restore(Integer id) {
        // TODO: Implement when needed
        return Uni.createFrom().failure(
                new UnsupportedOperationException("Restore not implemented yet")
        );
    }

    @Override
    public Uni<Boolean> voidEntry(Integer id) {
        // TODO: Implement when needed
        return Uni.createFrom().failure(
                new UnsupportedOperationException("Void entry not implemented yet")
        );
    }

    // ==================== JournalEntryQueryPort - Read Operations ====================

    @Override
    public Uni<java.util.Optional<JournalEntry>> findById(Integer id) {
        // TODO: Implement when needed
        return Uni.createFrom().failure(
                new UnsupportedOperationException("FindById not implemented yet")
        );
    }

    @Override
    public Uni<java.util.Optional<JournalEntry>> findByIdIncludingDeleted(Integer id) {
        // TODO: Implement when needed
        return Uni.createFrom().failure(
                new UnsupportedOperationException("FindByIdIncludingDeleted not implemented yet")
        );
    }

    @Override
    public Uni<Boolean> existsById(Integer id) {
        // TODO: Implement when needed
        return Uni.createFrom().failure(
                new UnsupportedOperationException("ExistsById not implemented yet")
        );
    }

    @Override
    public Uni<org.walrex.domain.model.PagedResult<JournalEntry>> findAll(
            org.walrex.application.dto.query.PageRequest pageRequest,
            org.walrex.application.dto.query.JournalEntryFilter filter) {
        // TODO: Implement when needed
        return Uni.createFrom().failure(
                new UnsupportedOperationException("FindAll not implemented yet")
        );
    }

    @Override
    public Uni<Long> count(org.walrex.application.dto.query.JournalEntryFilter filter) {
        // TODO: Implement when needed
        return Uni.createFrom().failure(
                new UnsupportedOperationException("Count not implemented yet")
        );
    }

    @Override
    public Uni<java.util.List<JournalEntry>> findAllWithFilter(
            org.walrex.application.dto.query.JournalEntryFilter filter) {
        // TODO: Implement when needed
        return Uni.createFrom().failure(
                new UnsupportedOperationException("FindAllWithFilter not implemented yet")
        );
    }

    @Override
    public Uni<java.util.List<JournalEntry>> findAllDeleted() {
        // TODO: Implement when needed
        return Uni.createFrom().failure(
                new UnsupportedOperationException("FindAllDeleted not implemented yet")
        );
    }

    // ==================== Correlative Generation ====================

    @Override
    public Uni<Integer> getNextBookCorrelative(String bookType, Integer year) {
        log.debug("Getting next book correlative for {} ({})", bookType, year);
        return queryRepository.getNextBookCorrelative(bookType, year);
    }

    @Override
    public Uni<Integer> getNextOperationNumber(Integer year) {
        log.debug("Getting next operation number for year {}", year);
        return queryRepository.getNextOperationNumber(year);
    }
}
