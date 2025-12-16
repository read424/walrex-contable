package org.walrex.infrastructure.adapter.inbound.rest.router;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.walrex.application.dto.query.CustomerFilter;
import org.walrex.application.dto.query.PageRequest;
import org.walrex.application.dto.request.CreateCustomerRequest;
import org.walrex.application.dto.request.UpdateCustomerRequest;
import org.walrex.application.dto.response.CustomerResponse;
import org.walrex.application.port.input.CreateCustomerUseCase;
import org.walrex.application.port.input.GetCustomerUseCase;
import org.walrex.application.port.input.ListCustomersUseCase;

import org.walrex.application.port.input.UpdateCustomerUseCase;
import org.walrex.domain.model.Customer;
import org.walrex.infrastructure.adapter.inbound.mapper.CustomerDtoMapper;
import org.walrex.infrastructure.adapter.inbound.mapper.CustomerRequestMapper;

import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class CustomerHandler {

    @Inject
    Validator validator;

    @Inject
    CustomerRequestMapper customerRequestMapper;

    @Inject
    CustomerDtoMapper customerDtoMapper;

    @Inject
    CreateCustomerUseCase createCustomerUseCase;

    @Inject
    UpdateCustomerUseCase updateCustomerUseCase;

    @Inject
    ListCustomersUseCase listCustomersUseCase;

    @Inject
    GetCustomerUseCase getCustomerUseCase;

    /**
     * POST /api/v1/customers - Create a new customer
     */
    @WithTransaction
    public Uni<Void> create(RoutingContext rc) {
        try {
            // Use rc.body() instead of rc.request().bodyHandler() to avoid "Request has
            // already been read" error
            CreateCustomerRequest request = rc.body().asPojo(CreateCustomerRequest.class);

            // Validate the request
            if (!validateCreateCustomerRequest(rc, request)) {
                return Uni.createFrom().voidItem(); // Validation failed, error response already sent
            }

            // Map request to domain model using MapStruct
            Customer customer = customerRequestMapper.toModel(request);

            return createCustomerUseCase.agregar(customer)
                    .onItem().invoke(customerDomain -> {
                        // Convert domain model to DTO response
                        CustomerResponse response = customerDtoMapper.toResponse(customerDomain);

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

    @WithTransaction
    public Uni<Void> update(RoutingContext rc) {
        try {
            String idParam = rc.pathParam("id");
            Integer id = Integer.parseInt(idParam);

            // Use rc.body() to read request body
            UpdateCustomerRequest request = rc.body().asPojo(UpdateCustomerRequest.class);

            // Validate the request
            if (!validateUpdateCustomerRequest(rc, request)) {
                return Uni.createFrom().voidItem();
            }

            // Map request to domain model
            Customer customer = customerRequestMapper.toModel(request);

            return updateCustomerUseCase.actualizar(id, customer)
                    .onItem().invoke(updatedCustomer -> {
                        // Convert domain model to DTO response
                        CustomerResponse response = customerDtoMapper.toResponse(updatedCustomer);

                        rc.response()
                                .setStatusCode(HttpResponseStatus.OK.code())
                                .putHeader("Content-Type", "application/json")
                                .end(Json.encode(response));
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
     * GET /api/v1/customers - List all customers with pagination and filtering
     */
    @WithSession
    public Uni<Void> list(RoutingContext rc) {
        try {
            // Parse query parameters
            // Frontend sends 1-based pages (page 1 is first), convert to 0-based for
            // backend
            int page = Math.max(0, getQueryParamAsInt(rc, "page", 1) - 1);
            int size = getQueryParamAsInt(rc, "size", 10);
            String sortBy = rc.queryParams().get("sortBy");
            String sortDirection = rc.queryParams().get("sortDirection");
            String search = rc.queryParams().get("search");
            String includeDeleted = rc.queryParams().get("includeDeleted");

            // Build filter using builder
            CustomerFilter filter = CustomerFilter.builder()
                    .search(search)
                    .includeDeleted(includeDeleted)
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
            return listCustomersUseCase.listar(pageRequest, filter)
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
     * GET /api/v1/customers/{id} - Get a customer by ID
     */
    @WithSession
    public Uni<Void> getById(RoutingContext rc) {
        try {
            String idParam = rc.pathParam("id");
            Integer id = Integer.parseInt(idParam);

            return getCustomerUseCase.findById(id)
                    .onItem().invoke(customer -> {
                        CustomerResponse response = customerDtoMapper.toResponse(customer);
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
     * Validates a CreateCustomerRequest using Jakarta Bean Validation.
     *
     * @param rc      The routing context for sending error responses
     * @param request The request object to validate
     * @return true if validation passes, false otherwise
     */
    private boolean validateCreateCustomerRequest(RoutingContext rc, CreateCustomerRequest request) {
        Set<ConstraintViolation<CreateCustomerRequest>> violations = validator.validate(request);

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
     * Validates a UpdateCustomerRequest using Jakarta Bean Validation.
     *
     * @param rc      The routing context for sending error responses
     * @param request The request object to validate
     * @return true if validation passes, false otherwise
     */
    private boolean validateUpdateCustomerRequest(RoutingContext rc, UpdateCustomerRequest request) {
        Set<ConstraintViolation<UpdateCustomerRequest>> violations = validator.validate(request);

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
        // Mapear excepciones de dominio a códigos HTTP
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

    /**
     * Helper method to parse query parameter as integer with default value.
     *
     * @param rc           The routing context
     * @param name         The parameter name
     * @param defaultValue The default value if parameter is missing or invalid
     * @return The parsed integer value or default
     */
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
}
