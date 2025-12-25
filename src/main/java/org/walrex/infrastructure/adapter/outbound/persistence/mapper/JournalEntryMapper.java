package org.walrex.infrastructure.adapter.outbound.persistence.mapper;

import org.mapstruct.*;
import org.walrex.domain.model.JournalEntry;
import org.walrex.domain.model.JournalEntryLine;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.JournalEntryEntity;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.JournalEntryLineEntity;

import java.util.List;

/**
 * Mapper between the domain model JournalEntry and the persistence entity JournalEntryEntity.
 *
 * Uses MapStruct to automatically generate mapping code at compile time.
 *
 * Following the hexagonal pattern, this mapper belongs to the infrastructure layer
 * as it knows both the domain model and persistence details.
 *
 * Configuration:
 * - componentModel = "cdi": Integration with Quarkus CDI (allows @Inject)
 * - unmappedTargetPolicy = IGNORE: Ignores unmapped fields
 * - injectionStrategy = CONSTRUCTOR: Uses constructor injection
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.CDI,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR
)
public interface JournalEntryMapper {

    /**
     * Converts a domain entity to a persistence entity.
     *
     * @param domain Domain model JournalEntry
     * @return JournalEntryEntity for persistence
     */
    @Mapping(target = "lines", ignore = true)
    JournalEntryEntity toEntity(JournalEntry domain);

    /**
     * Converts a persistence entity to a domain model.
     *
     * @param entity JournalEntryEntity from persistence
     * @return JournalEntry domain model
     */
    @Mapping(target = "lines", source = "lines")
    JournalEntry toDomain(JournalEntryEntity entity);

    /**
     * Converts a list of persistence entities to a list of domain models.
     *
     * @param entities List of JournalEntryEntity
     * @return List of JournalEntry
     */
    List<JournalEntry> toDomainList(List<JournalEntryEntity> entities);

    /**
     * Converts a list of domain models to a list of persistence entities.
     *
     * @param domains List of JournalEntry
     * @return List of JournalEntryEntity
     */
    List<JournalEntryEntity> toEntityList(List<JournalEntry> domains);

    /**
     * Converts a domain line to a persistence line entity.
     *
     * @param line Domain model JournalEntryLine
     * @return JournalEntryLineEntity for persistence
     */
    @Mapping(target = "journalEntry", ignore = true)
    JournalEntryLineEntity lineToEntity(JournalEntryLine line);

    /**
     * Converts a persistence line entity to a domain line.
     *
     * @param entity JournalEntryLineEntity from persistence
     * @return JournalEntryLine domain model
     */
    @Mapping(target = "journalEntryId", source = "journalEntry.id")
    JournalEntryLine lineToDomain(JournalEntryLineEntity entity);

    /**
     * Converts a list of domain lines to persistence line entities.
     *
     * @param lines List of JournalEntryLine
     * @return List of JournalEntryLineEntity
     */
    List<JournalEntryLineEntity> linesToEntityList(List<JournalEntryLine> lines);

    /**
     * Converts a list of persistence line entities to domain lines.
     *
     * @param entities List of JournalEntryLineEntity
     * @return List of JournalEntryLine
     */
    List<JournalEntryLine> linesToDomainList(List<JournalEntryLineEntity> entities);
}
