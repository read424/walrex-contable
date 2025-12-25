package org.walrex.infrastructure.adapter.inbound.mapper;

import org.mapstruct.*;
import org.walrex.application.dto.response.JournalEntryLineResponse;
import org.walrex.application.dto.response.JournalEntryResponse;
import org.walrex.domain.model.AccountingBookType;
import org.walrex.domain.model.EntryStatus;
import org.walrex.domain.model.JournalEntry;
import org.walrex.domain.model.JournalEntryLine;

import java.util.List;

/**
 * Mapper between JournalEntry domain model and application layer DTOs.
 *
 * Uses MapStruct to automatically generate mapping code at compile time.
 *
 * Responsibilities:
 * - Convert JournalEntry (domain) → JournalEntryResponse (DTO output)
 * - Handle type conversions (Enum → String)
 * - Calculate totals (totalDebit, totalCredit)
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.CDI,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR
)
public interface JournalEntryDtoMapper {

    /**
     * Converts domain model to response DTO.
     *
     * @param journalEntry Domain model
     * @return JournalEntryResponse DTO
     */
    @Mapping(target = "bookType", source = "bookType", qualifiedByName = "mapBookTypeToString")
    @Mapping(target = "status", source = "status", qualifiedByName = "mapStatusToString")
    @Mapping(target = "lines", source = "lines")
    @Mapping(target = "totalDebit", expression = "java(journalEntry.getTotalDebit())")
    @Mapping(target = "totalCredit", expression = "java(journalEntry.getTotalCredit())")
    JournalEntryResponse toResponse(JournalEntry journalEntry);

    /**
     * Converts a list of JournalEntry to JournalEntryResponse DTOs.
     *
     * @param journalEntries List of domain models
     * @return List of response DTOs
     */
    List<JournalEntryResponse> toResponseList(List<JournalEntry> journalEntries);

    /**
     * Converts JournalEntryLine domain model to response DTO.
     *
     * @param line JournalEntryLine domain model
     * @return JournalEntryLineResponse DTO
     */
    JournalEntryLineResponse lineToResponse(JournalEntryLine line);

    /**
     * Converts a list of JournalEntryLine to response DTOs.
     *
     * @param lines List of JournalEntryLine domain models
     * @return List of JournalEntryLineResponse DTOs
     */
    List<JournalEntryLineResponse> linesToResponseList(List<JournalEntryLine> lines);

    // ==================== Custom Conversion Methods ====================

    /**
     * Maps AccountingBookType enum to String.
     *
     * @param bookType AccountingBookType enum
     * @return String representation
     */
    @Named("mapBookTypeToString")
    default String mapBookTypeToString(AccountingBookType bookType) {
        if (bookType == null) {
            return null;
        }
        return bookType.name();
    }

    /**
     * Maps EntryStatus enum to String.
     *
     * @param status EntryStatus enum
     * @return String representation
     */
    @Named("mapStatusToString")
    default String mapStatusToString(EntryStatus status) {
        if (status == null) {
            return null;
        }
        return status.name();
    }
}
