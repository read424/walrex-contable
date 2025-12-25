package org.walrex.infrastructure.adapter.inbound.mapper;

import org.mapstruct.*;
import org.walrex.application.dto.request.CreateJournalEntryRequest;
import org.walrex.application.dto.request.JournalEntryLineRequest;
import org.walrex.domain.model.AccountingBookType;
import org.walrex.domain.model.JournalEntry;
import org.walrex.domain.model.JournalEntryLine;

import java.util.List;

/**
 * Mapper for converting JournalEntry request DTOs to domain models.
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.CDI,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR
)
public interface JournalEntryRequestMapper {

    /**
     * Converts CreateJournalEntryRequest to JournalEntry domain model.
     *
     * @param request DTO from REST handler
     * @return JournalEntry for use case
     */
    @Mapping(target = "bookType", source = "bookType", qualifiedByName = "mapBookType")
    @Mapping(target = "lines", source = "lines")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "operationNumber", ignore = true)
    @Mapping(target = "bookCorrelative", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    JournalEntry toModel(CreateJournalEntryRequest request);

    /**
     * Converts a list of JournalEntryLineRequest to JournalEntryLine domain models.
     *
     * @param lines List of line request DTOs
     * @return List of JournalEntryLine domain models
     */
    List<JournalEntryLine> linesToModel(List<JournalEntryLineRequest> lines);

    /**
     * Converts JournalEntryLineRequest to JournalEntryLine domain model.
     *
     * @param line Line request DTO
     * @return JournalEntryLine domain model
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "journalEntryId", ignore = true)
    JournalEntryLine lineToModel(JournalEntryLineRequest line);

    /**
     * Maps bookType string to AccountingBookType enum.
     *
     * @param bookType Book type as string (DIARIO, VENTAS, COMPRAS)
     * @return AccountingBookType enum
     */
    @Named("mapBookType")
    default AccountingBookType mapBookType(String bookType) {
        if (bookType == null || bookType.isBlank()) {
            return null;
        }
        return AccountingBookType.fromString(bookType);
    }
}
