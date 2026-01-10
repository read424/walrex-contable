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
import java.util.Optional;

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
    public Uni<Optional<JournalEntry>> findById(Integer id) {
        log.debug("Finding journal entry by id: {}", id);

        return repository.findById(id)
                .onItem().transform(entity -> {
                    if (entity == null) {
                        log.debug("Journal entry not found with id: {}", id);
                        return Optional.empty();
                    }

                    // Only return if not soft-deleted
                    if (entity.getDeletedAt() != null) {
                        log.debug("Journal entry {} is soft-deleted", id);
                        return Optional.empty();
                    }

                    log.debug("Found journal entry with id: {}", id);
                    return Optional.of(mapper.toDomain(entity));
                });
    }

    @Override
    public Uni<java.util.Optional<JournalEntry>> findByIdIncludingDeleted(Integer id) {
        log.debug("Finding journal entry by id (including deleted): {}", id);

        return repository.findById(id)
                .onItem().transform(entity -> {
                    if (entity == null) {
                        log.debug("Journal entry not found with id: {}", id);
                        return java.util.Optional.empty();
                    }

                    log.debug("Found journal entry with id: {} (deleted: {})", id, entity.getDeletedAt() != null);
                    return java.util.Optional.of(mapper.toDomain(entity));
                });
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
        log.debug("Finding all journal entries with pagination: page={}, size={}", pageRequest.getPage(), pageRequest.getSize());

        // Execute native SQL query and count in parallel
        Uni<java.util.List<Object[]>> entriesUni = queryRepository.findEntriesWithDetailsNative(
                filter,
                pageRequest.getPage(),
                pageRequest.getSize()
        );

        Uni<Long> countUni = queryRepository.countAll(filter);

        return Uni.combine().all().unis(entriesUni, countUni)
                .asTuple()
                .map(tuple -> {
                    java.util.List<Object[]> rows = tuple.getItem1();
                    Long totalElements = tuple.getItem2();

                    // Map SQL results to domain models
                    java.util.List<JournalEntry> journalEntries = rows.stream()
                            .map(this::mapRowToJournalEntry)
                            .toList();

                    return org.walrex.domain.model.PagedResult.of(
                            journalEntries,
                            pageRequest.getPage(),
                            pageRequest.getSize(),
                            totalElements
                    );
                })
                .invoke(result -> log.debug("Found {} journal entries out of {} total",
                        result.content().size(), result.totalElements()));
    }

    /**
     * Maps a SQL result row to JournalEntry domain model.
     * Row format: [id, entry_date, description, operation_number, book_correlative, book_type, status, created_at, updated_at, lines_json]
     */
    private JournalEntry mapRowToJournalEntry(Object[] row) {
        // Note: This is a simplified mapping - you'll need to parse the JSON and create proper domain objects
        // For now, return null lines - we'll implement full JSON parsing if needed
        return JournalEntry.builder()
                .id((Integer) row[0])
                .entryDate((java.time.LocalDate) row[1])
                .description((String) row[2])
                .operationNumber((Integer) row[3])
                .bookCorrelative((Integer) row[4])
                .bookType(org.walrex.domain.model.AccountingBookType.valueOf((String) row[5]))
                .status(org.walrex.domain.model.EntryStatus.valueOf((String) row[6]))
                .createdAt(row[7] != null ? ((java.sql.Timestamp) row[7]).toLocalDateTime().atOffset(java.time.ZoneOffset.UTC) : null)
                .updatedAt(row[8] != null ? ((java.sql.Timestamp) row[8]).toLocalDateTime().atOffset(java.time.ZoneOffset.UTC) : null)
                .lines(java.util.List.of()) // TODO: Parse JSON from row[9]
                .build();
    }

    @Override
    public Uni<Long> count(org.walrex.application.dto.query.JournalEntryFilter filter) {
        log.debug("Counting journal entries with filter: {}", filter);
        return queryRepository.countAll(filter);
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
