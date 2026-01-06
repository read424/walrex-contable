package org.walrex.infrastructure.adapter.inbound.rest.router;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.walrex.application.dto.request.CreateJournalEntryRequest;
import org.walrex.application.dto.request.JournalEntryLineRequest;
import org.walrex.application.dto.response.JournalEntryResponse;
import org.walrex.application.port.input.CreateJournalEntryUseCase;
import org.walrex.domain.exception.InvalidJournalEntryException;
import org.walrex.domain.exception.UnbalancedJournalEntryException;
import org.walrex.domain.model.JournalEntry;
import org.walrex.domain.model.JournalEntryDocument;
import org.walrex.domain.model.JournalEntryLine;
import org.walrex.infrastructure.adapter.inbound.mapper.JournalEntryDtoMapper;
import org.walrex.infrastructure.adapter.inbound.mapper.JournalEntryRequestMapper;
import org.walrex.infrastructure.adapter.inbound.rest.service.DocumentProcessorService;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Handler for JournalEntry REST operations.
 * Handles HTTP request/response logic and delegates business logic to use cases.
 */
@Slf4j
@ApplicationScoped
public class JournalEntryHandler {

    @Inject
    CreateJournalEntryUseCase createJournalEntryUseCase;

    @Inject
    Validator validator;

    @Inject
    JournalEntryRequestMapper journalEntryRequestMapper;

    @Inject
    JournalEntryDtoMapper journalEntryDtoMapper;

    @Inject
    DocumentProcessorService documentProcessorService;

    @Inject
    org.walrex.application.port.input.SyncHistoricalEntriesUseCase syncHistoricalEntriesUseCase;

    @Inject
    org.walrex.application.port.output.JournalEntryQueryPort journalEntryQueryPort;

    /**
     * POST /api/v1/journal-entries - Create a new journal entry
     */
    @WithTransaction
    public Uni<Void> create(RoutingContext rc) {
        try {
            log.debug("Received request to create journal entry");

            // Parse request body
            CreateJournalEntryRequest request = rc.body().asPojo(CreateJournalEntryRequest.class);

            // Validate the request
            if (!validateCreateRequest(rc, request)) {
                return Uni.createFrom().voidItem(); // Validation failed, error response already sent
            }

            // Process documents for each line (decode base64 and store files)
            return processLineDocuments(request.lines())
                    // Map request to domain model with processed documents
                    .onItem().transform(processedLines -> {
                        JournalEntry journalEntry = journalEntryRequestMapper.toModel(request);
                        journalEntry.setLines(processedLines);
                        return journalEntry;
                    })
                    // Execute use case
                    .onItem().transformToUni(journalEntry -> createJournalEntryUseCase.execute(journalEntry))
                    .onItem().invoke(savedJournalEntry -> {
                        // Sync to Qdrant asynchronously (fire-and-forget)
                        syncHistoricalEntriesUseCase.syncEntry(savedJournalEntry.getId())
                                .subscribe().with(
                                        success -> log.info("Journal entry {} synced to Qdrant for RAG", savedJournalEntry.getId()),
                                        failure -> log.error("Failed to sync journal entry {} to Qdrant", savedJournalEntry.getId(), failure)
                                );

                        // Convert domain model to DTO response
                        JournalEntryResponse response = journalEntryDtoMapper.toResponse(savedJournalEntry);

                        String location = rc.request().absoluteURI() + "/" + response.id();
                        rc.response()
                                .setStatusCode(HttpResponseStatus.CREATED.code())
                                .putHeader("Content-Type", "application/json")
                                .putHeader("Location", location)
                                .end(Json.encode(response));

                        log.info("Journal entry created successfully with id: {}", response.id());
                    })
                    .onFailure().invoke(error -> handleError(rc, error))
                    .replaceWithVoid();
        } catch (Exception e) {
            log.error("Error parsing request body", e);
            handleBadRequest(rc, "Invalid request body: " + e.getMessage());
            return Uni.createFrom().voidItem();
        }
    }

    /**
     * Processes documents for all lines.
     * Decodes base64, stores files, and creates JournalEntryLine with documents.
     */
    private Uni<List<JournalEntryLine>> processLineDocuments(List<JournalEntryLineRequest> lineRequests) {
        if (lineRequests == null || lineRequests.isEmpty()) {
            return Uni.createFrom().item(new ArrayList<>());
        }

        List<Uni<JournalEntryLine>> lineUnis = lineRequests.stream()
                .map(this::processLinewithDocuments)
                .toList();

        return Uni.combine().all().unis(lineUnis).with(list -> {
            List<JournalEntryLine> result = new ArrayList<>();
            for (Object obj : list) {
                result.add((JournalEntryLine) obj);
            }
            return result;
        });
    }

    /**
     * Processes a single line with its documents.
     */
    private Uni<JournalEntryLine> processLinewithDocuments(JournalEntryLineRequest lineRequest) {
        // Map basic line properties
        JournalEntryLine line = journalEntryRequestMapper.lineToModel(lineRequest);

        // Process documents if present
        if (lineRequest.documents() != null && !lineRequest.documents().isEmpty()) {
            return documentProcessorService.processDocuments(lineRequest.documents())
                    .onItem().transform(documents -> {
                        line.setDocuments(documents);
                        return line;
                    });
        }

        // No documents, return line as-is
        return Uni.createFrom().item(line);
    }

    // ==================== Validation Methods ====================

    /**
     * Validates CreateJournalEntryRequest using Jakarta Validation.
     *
     * @return true if valid, false if invalid (response already sent)
     */
    private boolean validateCreateRequest(RoutingContext rc, CreateJournalEntryRequest request) {
        Set<ConstraintViolation<CreateJournalEntryRequest>> violations = validator.validate(request);

        if (!violations.isEmpty()) {
            String errorMessage = violations.stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.joining(", "));

            log.warn("Validation failed for create journal entry request: {}", errorMessage);
            handleBadRequest(rc, errorMessage);
            return false;
        }

        return true;
    }

    // ==================== Error Handling Methods ====================

    /**
     * Handles errors based on exception type.
     */
    private void handleError(RoutingContext rc, Throwable error) {
        log.error("Error handling journal entry request", error);

        if (error instanceof UnbalancedJournalEntryException unbalancedEx) {
            sendJson(rc, HttpResponseStatus.BAD_REQUEST, new ErrorResponse(
                    "UNBALANCED_ENTRY",
                    error.getMessage(),
                    new ErrorDetails(
                            unbalancedEx.getTotalDebit().toString(),
                            unbalancedEx.getTotalCredit().toString(),
                            unbalancedEx.getDifference().toString()
                    )
            ));
        } else if (error instanceof InvalidJournalEntryException invalidEx) {
            sendJson(rc, HttpResponseStatus.BAD_REQUEST, new ErrorResponse(
                    "INVALID_ENTRY",
                    error.getMessage(),
                    invalidEx.getField()
            ));
        } else {
            sendJson(rc, HttpResponseStatus.INTERNAL_SERVER_ERROR, new ErrorResponse(
                    "INTERNAL_ERROR",
                    "An unexpected error occurred: " + error.getMessage(),
                    null
            ));
        }
    }

    private void handleBadRequest(RoutingContext rc, String message) {
        sendJson(rc, HttpResponseStatus.BAD_REQUEST, new ErrorResponse(
                "BAD_REQUEST",
                message,
                null
        ));
    }

    private void sendJson(RoutingContext rc, HttpResponseStatus status, Object body) {
        rc.response()
                .setStatusCode(status.code())
                .putHeader("Content-Type", "application/json")
                .end(Json.encode(body));
    }

    /**
     * GET /api/v1/journal-entries - List journal entries with pagination
     */
    @WithSession
    public Uni<Void> list(RoutingContext rc) {
        try {
            log.debug("Received request to list journal entries");

            // Parse query parameters
            int page = parseIntParam(rc, "page", 1); // 1-indexed for frontend
            int size = parseIntParam(rc, "size", 10);
            Integer year = parseIntParamOptional(rc, "year");
            Integer month = parseIntParamOptional(rc, "month");
            String bookType = rc.request().getParam("bookType");
            String status = rc.request().getParam("status");
            String search = rc.request().getParam("search");

            // Validate page and size
            if (page < 1) {
                handleBadRequest(rc, "Page must be >= 1");
                return Uni.createFrom().voidItem();
            }
            if (size < 1 || size > 100) {
                handleBadRequest(rc, "Size must be between 1 and 100");
                return Uni.createFrom().voidItem();
            }

            // Build filter
            var filter = org.walrex.application.dto.query.JournalEntryFilter.builder()
                    .year(year)
                    .month(month)
                    .bookType(bookType)
                    .status(status)
                    .search(search)
                    .includeDeleted("0") // Don't include deleted by default
                    .build();

            // Build page request (convert to 0-indexed for backend)
            var pageRequest = org.walrex.application.dto.query.PageRequest.builder()
                    .page(page - 1) // Convert from 1-indexed to 0-indexed
                    .size(size)
                    .sortBy("entryDate")
                    .sortDirection(org.walrex.application.dto.query.PageRequest.SortDirection.DESCENDING)
                    .build();

            // Query journal entries
            return journalEntryQueryPort.findAll(pageRequest, filter)
                    .onItem().invoke(pagedResult -> {
                        // Convert domain models to DTOs
                        var journalEntryResponses = pagedResult.content().stream()
                                .map(journalEntryDtoMapper::toResponse)
                                .toList();

                        // Create paged response (convert back to 1-indexed for frontend)
                        var response = org.walrex.application.dto.response.PagedResponse.of(
                                journalEntryResponses,
                                page, // Use original 1-indexed page
                                size,
                                pagedResult.totalElements()
                        );

                        rc.response()
                                .setStatusCode(200)
                                .putHeader("Content-Type", "application/json")
                                .end(Json.encode(response));

                        log.info("Listed {} journal entries (page {}/{})",
                                response.content().size(), response.page(), response.totalPages());
                    })
                    .onFailure().invoke(error -> handleError(rc, error))
                    .replaceWithVoid();
        } catch (Exception e) {
            log.error("Error parsing query parameters", e);
            handleBadRequest(rc, "Invalid query parameters: " + e.getMessage());
            return Uni.createFrom().voidItem();
        }
    }

    // ==================== Helper Methods ====================

    /**
     * Parses an integer parameter from query string with default value.
     */
    private int parseIntParam(RoutingContext rc, String name, int defaultValue) {
        String value = rc.request().getParam(name);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid " + name + ": " + value);
        }
    }

    /**
     * Parses an optional integer parameter from query string.
     */
    private Integer parseIntParamOptional(RoutingContext rc, String name) {
        String value = rc.request().getParam(name);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid " + name + ": " + value);
        }
    }

    // ==================== Helper Classes ====================

    private record ErrorResponse(String code, String message, Object details) {
    }

    private record ErrorDetails(String totalDebit, String totalCredit, String difference) {
    }
}
