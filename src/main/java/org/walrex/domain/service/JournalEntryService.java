package org.walrex.domain.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.walrex.application.dto.query.JournalEntryFilter;
import org.walrex.application.dto.query.PageRequest;
import org.walrex.application.dto.response.JournalEntryResponse;
import org.walrex.application.dto.response.PagedResponse;
import org.walrex.application.port.input.*;
import org.walrex.application.port.output.JournalEntryQueryPort;
import org.walrex.application.port.output.JournalEntryRepositoryPort;
import org.walrex.domain.exception.InvalidJournalEntryException;
import org.walrex.domain.exception.JournalEntryNotFoundException;
import org.walrex.domain.exception.UnbalancedJournalEntryException;
import org.walrex.domain.model.AccountingBookType;
import org.walrex.domain.model.EntryStatus;
import org.walrex.domain.model.JournalEntry;
import org.walrex.infrastructure.adapter.inbound.mapper.JournalEntryDtoMapper;

import java.math.BigDecimal;

@Slf4j
@Transactional
@ApplicationScoped
public class JournalEntryService implements
        CreateJournalEntryUseCase,
        GetJournalEntryUseCase,
        ListJournalEntriesUseCase,
        UpdateJournalEntryUseCase,
        DeleteJournalEntryUseCase {

    @Inject
    JournalEntryRepositoryPort journalEntryRepositoryPort;

    @Inject
    JournalEntryQueryPort journalEntryQueryPort;

    @Inject
    JournalEntryDtoMapper journalEntryDtoMapper;

    // ==================== CreateJournalEntryUseCase ====================

    /**
     * Creates a new journal entry.
     *
     * @throws UnbalancedJournalEntryException if total debits != total credits
     * @throws InvalidJournalEntryException if the entry doesn't meet business rules
     */
    @Override
    public Uni<JournalEntry> execute(JournalEntry journalEntry) {
        log.info("Creating journal entry: {}", journalEntry.getDescription());

        // 1. Validate business rules
        return validateJournalEntry(journalEntry)
                // 2. Generate correlatives
                .call(je -> generateCorrelatives(je))
                // 3. Set default status if not set
                .invoke(je -> {
                    if (je.getStatus() == null) {
                        je.setStatus(EntryStatus.ACTIVE);
                    }
                })
                // 4. Save to database
                .onItem().transformToUni(je -> journalEntryRepositoryPort.save(je))
                .invoke(savedEntry -> log.info("Journal entry created with id: {}", savedEntry.getId()));
    }

    // ==================== GetJournalEntryUseCase ====================

    /**
     * Gets a journal entry by its ID.
     *
     * @throws JournalEntryNotFoundException if no journal entry exists with the provided ID
     */
    @Override
    public Uni<JournalEntry> findById(Integer id) {
        log.info("Getting journal entry by id: {}", id);
        return journalEntryQueryPort.findById(id)
                .onItem().transform(optional -> optional.orElseThrow(
                        () -> new JournalEntryNotFoundException(id)
                ));
    }

    // ==================== ListJournalEntriesUseCase ====================

    /**
     * Lists journal entries with pagination and filters.
     */
    @Override
    public Uni<PagedResponse<JournalEntryResponse>> execute(PageRequest pageRequest, JournalEntryFilter filter) {
        log.info("Listing journal entries with page: {}, size: {}, filter: {}",
                pageRequest.getPage(), pageRequest.getSize(), filter);
        /*
        return journalEntryQueryPort.findAll(pageRequest, filter)
                .onItem().transform(pagedResult -> {
                    var responses = journalEntryDtoMapper.toResponseList(pagedResult.getItems());
                    return new PagedResponse<>(
                            responses,
                            pagedResult.getPage(),
                            pagedResult.getSize(),
                            pagedResult.getTotalElements(),
                            pagedResult.getTotalPages()
                    );
                });
         */
        return null;
    }

    // ==================== UpdateJournalEntryUseCase ====================

    /**
     * Updates an existing journal entry.
     *
     * @throws JournalEntryNotFoundException if no journal entry exists with the provided ID
     * @throws UnbalancedJournalEntryException if total debits != total credits
     * @throws InvalidJournalEntryException if the entry doesn't meet business rules
     */
    @Override
    public Uni<JournalEntry> execute(Integer id, JournalEntry journalEntry) {
        log.info("Updating journal entry id: {}", id);

        // 1. Check if entry exists
        return findById(id)
                // 2. Validate business rules
                .call(existingEntry -> validateJournalEntry(journalEntry))
                // 3. Set the ID
                .invoke(existingEntry -> journalEntry.setId(id))
                // 4. Update in database
                .onItem().transformToUni(existingEntry -> journalEntryRepositoryPort.update(journalEntry))
                .invoke(updatedEntry -> log.info("Journal entry {} updated", id));
    }

    // ==================== DeleteJournalEntryUseCase ====================

    /**
     * Soft deletes a journal entry (sets deletedAt timestamp).
     */
    @Override
    public Uni<Boolean> execute(Integer id) {
        log.info("Soft deleting journal entry id: {}", id);

        // Check if entry exists first
        return findById(id)
                .onItem().transformToUni(entry -> journalEntryRepositoryPort.softDelete(id))
                .invoke(deleted -> {
                    if (deleted) {
                        log.info("Journal entry {} soft deleted", id);
                    } else {
                        log.warn("Failed to soft delete journal entry {}", id);
                    }
                });
    }

    /**
     * Restores a soft-deleted journal entry.
     */
    @Override
    public Uni<Boolean> restore(Integer id) {
        log.info("Restoring journal entry id: {}", id);

        return journalEntryRepositoryPort.restore(id)
                .invoke(restored -> {
                    if (restored) {
                        log.info("Journal entry {} restored", id);
                    } else {
                        log.warn("Failed to restore journal entry {} (might not exist or not deleted)", id);
                    }
                });
    }

    /**
     * Voids a journal entry (sets status to VOIDED).
     * This is the accounting way to "cancel" an entry without deleting it.
     */
    @Override
    public Uni<Boolean> voidEntry(Integer id) {
        log.info("Voiding journal entry id: {}", id);

        // Check if entry exists first
        return findById(id)
                .invoke(entry -> {
                    if (entry.isVoided()) {
                        log.warn("Journal entry {} is already voided", id);
                    }
                })
                .onItem().transformToUni(entry -> journalEntryRepositoryPort.voidEntry(id))
                .invoke(voided -> {
                    if (voided) {
                        log.info("Journal entry {} voided", id);
                    } else {
                        log.warn("Failed to void journal entry {}", id);
                    }
                });
    }

    // ==================== Private Validation Methods ====================

    /**
     * Validates all business rules for a journal entry.
     */
    private Uni<JournalEntry> validateJournalEntry(JournalEntry journalEntry) {
        // 1. Check minimum lines
        if (!journalEntry.hasMinimumLines()) {
            return Uni.createFrom().failure(
                    new InvalidJournalEntryException(
                            "lines",
                            "A journal entry must have at least 2 lines"
                    )
            );
        }

        // 2. Check balance
        if (!journalEntry.isBalanced()) {
            BigDecimal totalDebit = journalEntry.getTotalDebit();
            BigDecimal totalCredit = journalEntry.getTotalCredit();
            return Uni.createFrom().failure(
                    new UnbalancedJournalEntryException(totalDebit, totalCredit)
            );
        }

        // 3. Validate each line
        for (var line : journalEntry.getLines()) {
            if (!line.isValid()) {
                return Uni.createFrom().failure(
                        new InvalidJournalEntryException(
                                "line",
                                "Each line must have at least debit or credit greater than zero"
                        )
                );
            }

            // Validate that debit and credit are not both greater than zero
            if (line.getDebit().compareTo(BigDecimal.ZERO) > 0 &&
                    line.getCredit().compareTo(BigDecimal.ZERO) > 0) {
                return Uni.createFrom().failure(
                        new InvalidJournalEntryException(
                                "line",
                                "A line cannot have both debit and credit greater than zero"
                        )
                );
            }
        }

        // 4. Validate book type
        try {
            AccountingBookType.fromString(journalEntry.getBookType().name());
        } catch (IllegalArgumentException e) {
            return Uni.createFrom().failure(
                    new InvalidJournalEntryException("bookType", e.getMessage())
            );
        }

        // All validations passed
        return Uni.createFrom().item(journalEntry);
    }

    /**
     * Generates correlatives for the journal entry.
     * - operation_number: general correlative per year
     * - book_correlative: correlative per book type and year
     */
    private Uni<Void> generateCorrelatives(JournalEntry journalEntry) {
        Integer year = journalEntry.getEntryDate().getYear();
        String bookType = journalEntry.getBookType().name();

        // Generate both correlatives in parallel
        Uni<Integer> operationNumberUni = journalEntryQueryPort.getNextOperationNumber(year);
        Uni<Integer> bookCorrelativeUni = journalEntryQueryPort.getNextBookCorrelative(bookType, year);

        return Uni.combine().all().unis(operationNumberUni, bookCorrelativeUni)
                .asTuple()
                .invoke(tuple -> {
                    journalEntry.setOperationNumber(tuple.getItem1());
                    journalEntry.setBookCorrelative(tuple.getItem2());
                    log.debug("Generated correlatives - Operation: {}, Book: {}",
                            tuple.getItem1(), tuple.getItem2());
                })
                .replaceWithVoid();
    }
}
