package org.walrex.infrastructure.adapter.inbound.rest.router;

import io.netty.handler.codec.http.HttpResponseStatus;
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
import org.walrex.application.dto.response.JournalEntryResponse;
import org.walrex.application.port.input.CreateJournalEntryUseCase;
import org.walrex.domain.exception.InvalidJournalEntryException;
import org.walrex.domain.exception.UnbalancedJournalEntryException;
import org.walrex.domain.model.JournalEntry;
import org.walrex.infrastructure.adapter.inbound.mapper.JournalEntryDtoMapper;
import org.walrex.infrastructure.adapter.inbound.mapper.JournalEntryRequestMapper;

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

            // Map request to domain model using MapStruct
            JournalEntry journalEntry = journalEntryRequestMapper.toModel(request);

            // Execute use case
            return createJournalEntryUseCase.execute(journalEntry)
                    .onItem().invoke(savedJournalEntry -> {
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

    // ==================== Helper Classes ====================

    private record ErrorResponse(String code, String message, Object details) {
    }

    private record ErrorDetails(String totalDebit, String totalCredit, String difference) {
    }
}
