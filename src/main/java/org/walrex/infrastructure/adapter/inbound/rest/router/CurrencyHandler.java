package org.walrex.infrastructure.adapter.inbound.rest.router;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.walrex.application.dto.query.CurrencyFilter;
import org.walrex.application.dto.query.PageRequest;
import org.walrex.application.dto.request.CreateCurrencyRequest;
import org.walrex.application.dto.request.UpdateCurrencyRequest;
import org.walrex.application.dto.response.CurrencyResponse;
import org.walrex.application.port.input.*;
import org.walrex.domain.model.Currency;
import org.walrex.infrastructure.adapter.inbound.mapper.CurrencyDtoMapper;
import org.walrex.infrastructure.adapter.inbound.mapper.CurrencyRequestMapper;

import java.util.Set;
import java.util.stream.Collectors;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class CurrencyHandler {

    @Inject
    CreateCurrencyUseCase createCurrencyUseCase;

    @Inject
    GetCurrencyUseCase getCurrencyUseCase;

    @Inject
    ListCurrenciesUseCase listCurrenciesUseCase;

    @Inject
    UpdateCurrencyUseCase updateCurrencyUseCase;

    @Inject
    DeleteCurrencyUseCase deleteCurrencyUseCase;

    @Inject
    CheckAvailabilityUseCase checkAvailabilityUseCase;

    @Inject
    Validator validator;

    @Inject
    CurrencyRequestMapper currencyRequestMapper;

    @Inject
    CurrencyDtoMapper currencyDtoMapper;

    @WithTransaction
    public Uni<Void> create(RoutingContext rc) {
        try {
            // Use rc.body() instead of rc.request().bodyHandler() to avoid "Request has
            // already been read" error
            CreateCurrencyRequest request = rc.body().asPojo(CreateCurrencyRequest.class);

            // Validate the request
            if (!validateCreateCurrencyRequest(rc, request)) {
                return Uni.createFrom().voidItem(); // Validation failed, error response already sent
            }

            // Map request to command using MapStruct
            Currency currency = currencyRequestMapper.toModel(request);

            return createCurrencyUseCase.execute(currency)
                    .onItem().invoke(currencyDomain -> {
                        // Convert domain model to DTO response
                        CurrencyResponse response = currencyDtoMapper.toResponse(currencyDomain);

                        String location = rc.request().absoluteURI() + "/" + response.id();
                        rc.response()
                                .setStatusCode(HttpResponseStatus.CREATED.code())
                                .putHeader("Content-Type", "application/json")
                                .putHeader("Location", location)
                                .end(Json.encode(response));
                    })
                    .onFailure().invoke(error -> handleError(rc, error))
                    .replaceWithVoid();
        } catch (Exception e) {
            handleBadRequest(rc, "Invalid request body: " + e.getMessage());
            return Uni.createFrom().voidItem();
        }
    }

    /**
     * GET /api/v1/currencies - List all currencies with pagination and filtering
     */
    @WithSession
    public Uni<Void> list(RoutingContext rc) {
        try {
            // Parse query parameters
            // Frontend sends 1-based pages (page 1 is first), convert to 0-based for
            // backend
            int page = Math.max(0, getQueryParamAsInt(rc, "page", 1) - 1);
            int size = getQueryParamAsInt(rc, "size", 20);
            String sortBy = rc.queryParams().get("sortBy");
            String sortDirection = rc.queryParams().get("sortDirection");
            String status = rc.queryParams().get("active");
            String search = rc.queryParams().get("search");

            // Build filter using builder
            CurrencyFilter filter = CurrencyFilter.builder()
                    .search(search)
                    .status(status)
                    .includeDeleted("1")
                    .build();

            // Build page request using builder
            PageRequest.SortDirection direction = sortDirection != null
                    ? PageRequest.SortDirection.fromString(sortDirection)
                    : PageRequest.SortDirection.ASCENDING;

            PageRequest.PageRequestBuilder pageRequestBuilder = PageRequest.builder()
                    .page(page)
                    .size(size)
                    .sortDirection(direction);

            if (sortBy != null && !sortBy.isBlank()) {
                pageRequestBuilder.sortBy(sortBy);
            }

            PageRequest pageRequest = pageRequestBuilder.build();

            // Execute use case
            return listCurrenciesUseCase.execute(pageRequest, filter)
                    .onItem().invoke(pagedResponse -> {
                        sendJson(rc, HttpResponseStatus.OK, pagedResponse);
                    })
                    .onFailure().invoke(error -> handleError(rc, error))
                    .replaceWithVoid();
        } catch (Exception e) {
            handleBadRequest(rc, "Invalid query parameters: " + e.getMessage());
            return Uni.createFrom().voidItem();
        }
    }

    /**
     * GET /api/v1/currencies/all - List all currencies without pagination
     */
    @WithSession
    public Uni<Void> findAll(RoutingContext rc) {
        try {
            // Parse query parameters
            String search = rc.queryParams().get("search");
            String includeInactive = rc.queryParams().get("includeInactive");

            // Por defecto, solo monedas activas
            String status = "1";

            // Si includeInactive es true, no filtrar por status
            if ("true".equalsIgnoreCase(includeInactive)) {
                status = null;
            }

            // Build filter using builder
            CurrencyFilter filter = CurrencyFilter.builder()
                    .search(search)
                    .status(status)
                    .build();

            // Execute use case
            return listCurrenciesUseCase.findAll(filter)
                    .onItem().invoke(currencies -> {
                        sendJson(rc, HttpResponseStatus.OK, currencies);
                    })
                    .onFailure().invoke(error -> handleError(rc, error))
                    .replaceWithVoid();
        } catch (Exception e) {
            handleBadRequest(rc, "Invalid query parameters: " + e.getMessage());
            return Uni.createFrom().voidItem();
        }
    }

    /**
     * GET /api/v1/currencies/{id} - Get a currency by ID
     */
    @WithSession
    public Uni<Void> getById(RoutingContext rc) {
        try {
            String idParam = rc.pathParam("id");
            Integer id = Integer.parseInt(idParam);

            return getCurrencyUseCase.findById(id)
                    .onItem().invoke(currency -> {
                        CurrencyResponse response = currencyDtoMapper.toResponse(currency);
                        sendJson(rc, HttpResponseStatus.OK, response);
                    })
                    .onFailure().invoke(error -> handleError(rc, error))
                    .replaceWithVoid();
        } catch (NumberFormatException e) {
            handleBadRequest(rc, "Invalid ID format. ID must be a number.");
            return Uni.createFrom().voidItem();
        }
    }

    /**
     * PUT /api/v1/currencies/{id} - Update a currency
     */
    @WithTransaction
    public Uni<Void> update(RoutingContext rc) {
        try {
            String idParam = rc.pathParam("id");
            Integer id = Integer.parseInt(idParam);

            // Use rc.body() instead of rc.request().bodyHandler() to avoid "Request has
            // already been read" error
            UpdateCurrencyRequest request = rc.body().asPojo(UpdateCurrencyRequest.class);

            // Validate the request
            if (!validateUpdateCurrencyRequest(rc, request)) {
                return Uni.createFrom().voidItem(); // Validation failed, error response already sent
            }

            // Map request to domain model
            Currency currency = currencyRequestMapper.toModel(request);

            return updateCurrencyUseCase.execute(id, currency)
                    .onItem().invoke(updatedCurrency -> {
                        CurrencyResponse response = currencyDtoMapper.toResponse(updatedCurrency);
                        sendJson(rc, HttpResponseStatus.OK, response);
                    })
                    .onFailure().invoke(error -> handleError(rc, error))
                    .replaceWithVoid();
        } catch (NumberFormatException e) {
            handleBadRequest(rc, "Invalid ID format. ID must be a number.");
            return Uni.createFrom().voidItem();
        } catch (Exception e) {
            handleBadRequest(rc, "Invalid request body: " + e.getMessage());
            return Uni.createFrom().voidItem();
        }
    }

    /**
     * DELETE /api/v1/currencies/{id} - Delete a currency (soft delete)
     */
    @WithTransaction
    public Uni<Void> delete(RoutingContext rc) {
        try {
            String idParam = rc.pathParam("id");
            Integer id = Integer.parseInt(idParam);

            return deleteCurrencyUseCase.execute(id)
                    .onItem().invoke(voidItem -> {
                        rc.response()
                                .setStatusCode(HttpResponseStatus.NO_CONTENT.code())
                                .end();
                    })
                    .onFailure().invoke(error -> handleError(rc, error))
                    .replaceWithVoid();
        } catch (NumberFormatException e) {
            handleBadRequest(rc, "Invalid ID format. ID must be a number.");
            return Uni.createFrom().voidItem();
        }
    }

    /**
     * GET /api/v1/currencies/check-availability - Check if a field value is
     * available
     * Query params: alphabeticCode, numericCode, or name
     * Optional: excludeId (to exclude a specific ID from the check, useful for
     * updates)
     */
    @WithSession
    public Uni<Void> checkAvailability(RoutingContext rc) {
        String alphabeticCode = rc.queryParams().get("alphabeticCode");
        String numericCode = rc.queryParams().get("numericCode");
        String name = rc.queryParams().get("name");
        String excludeIdParam = rc.queryParams().get("excludeId");

        // Parse excludeId if provided
        Integer excludeId = null;
        if (excludeIdParam != null && !excludeIdParam.isBlank()) {
            try {
                excludeId = Integer.parseInt(excludeIdParam);
            } catch (NumberFormatException e) {
                handleBadRequest(rc, "Invalid excludeId format. It must be a number.");
                return Uni.createFrom().voidItem();
            }
        }

        // Validate that at least one parameter is provided
        if (isBlank(alphabeticCode) && isBlank(numericCode) && isBlank(name)) {
            handleBadRequest(rc, "At least one query parameter is required: alphabeticCode, numericCode, or name");
            return Uni.createFrom().voidItem();
        }

        // Check which field to validate (priority: alphabeticCode > numericCode > name)
        final Integer finalExcludeId = excludeId;
        Uni<org.walrex.application.dto.response.AvailabilityResponse> checkUni;

        if (!isBlank(alphabeticCode)) {
            checkUni = checkAvailabilityUseCase.checkAlphabeticCode(alphabeticCode.trim().toUpperCase(),
                    finalExcludeId);
        } else if (!isBlank(numericCode)) {
            checkUni = checkAvailabilityUseCase.checkNumericCode(numericCode.trim(), finalExcludeId);
        } else {
            checkUni = checkAvailabilityUseCase.checkName(name.trim(), finalExcludeId);
        }

        return checkUni
                .onItem().invoke(available -> sendJson(rc, HttpResponseStatus.OK, available))
                .onFailure().invoke(error -> handleError(rc, error))
                .replaceWithVoid();
    }

    /**
     * Validates a CreateCurrencyRequest using Jakarta Bean Validation.
     *
     * @param rc      The routing context for sending error responses
     * @param request The request object to validate
     * @return true if validation passes, false otherwise
     */
    private boolean validateCreateCurrencyRequest(RoutingContext rc, CreateCurrencyRequest request) {
        Set<ConstraintViolation<CreateCurrencyRequest>> violations = validator.validate(request);

        if (!violations.isEmpty()) {
            String errorMessage = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining(", "));

            handleValidationError(rc, errorMessage);
            return false;
        }

        return true;
    }

    /**
     * Validates an UpdateCurrencyRequest using Jakarta Bean Validation.
     *
     * @param rc      The routing context for sending error responses
     * @param request The request object to validate
     * @return true if validation passes, false otherwise
     */
    private boolean validateUpdateCurrencyRequest(RoutingContext rc, UpdateCurrencyRequest request) {
        Set<ConstraintViolation<UpdateCurrencyRequest>> violations = validator.validate(request);

        if (!violations.isEmpty()) {
            String errorMessage = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining(", "));

            handleValidationError(rc, errorMessage);
            return false;
        }

        return true;
    }

    /**
     * Handles validation errors by sending a BAD_REQUEST response with validation
     * messages.
     *
     * @param rc               The routing context
     * @param validationErrors The validation error messages
     */
    private void handleValidationError(RoutingContext rc, String validationErrors) {
        JsonObject error = new JsonObject()
                .put("status", HttpResponseStatus.BAD_REQUEST.code())
                .put("error", HttpResponseStatus.BAD_REQUEST.reasonPhrase())
                .put("message", "Validation failed: " + validationErrors)
                .put("path", rc.request().path());

        rc.response()
                .setStatusCode(HttpResponseStatus.BAD_REQUEST.code())
                .putHeader("Content-Type", "application/json")
                .end(error.encode());
    }

    private void sendJson(RoutingContext rc, HttpResponseStatus status, Object body) {
        rc.response()
                .setStatusCode(status.code())
                .putHeader("Content-Type", "application/json")
                .end(Json.encode(body));
    }

    private void handleError(RoutingContext rc, Throwable error) {
        // Aquí puedes mapear excepciones de dominio a códigos HTTP
        String errorName = error.getClass().getSimpleName();

        HttpResponseStatus status;
        if (errorName.contains("NotFound")) {
            status = HttpResponseStatus.NOT_FOUND;
        } else if (errorName.contains("Duplicate") || errorName.contains("Conflict")) {
            status = HttpResponseStatus.CONFLICT;
        } else if (errorName.contains("Invalid") || errorName.contains("IllegalArgument")) {
            status = HttpResponseStatus.BAD_REQUEST;
        } else {
            status = HttpResponseStatus.INTERNAL_SERVER_ERROR;
        }

        sendErrorResponse(rc, status, error.getMessage());
    }

    private void handleBadRequest(RoutingContext rc, String message) {
        sendErrorResponse(rc, HttpResponseStatus.BAD_REQUEST, message);
    }

    private void sendErrorResponse(RoutingContext rc, HttpResponseStatus status, String message) {
        JsonObject error = new JsonObject()
                .put("status", status.code())
                .put("error", status.reasonPhrase())
                .put("message", message)
                .put("path", rc.request().path());

        rc.response()
                .setStatusCode(status.code())
                .putHeader("Content-Type", "application/json")
                .end(error.encode());
    }

    private int getQueryParamAsInt(RoutingContext rc, String name, int defaultValue) {
        String value = rc.queryParams().get(name);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private Boolean getQueryParamAsBoolean(RoutingContext rc, String name, Boolean defaultValue) {
        String value = rc.queryParams().get(name);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }

    private boolean isBlank(String str) {
        return str == null || str.isBlank();
    }
}
