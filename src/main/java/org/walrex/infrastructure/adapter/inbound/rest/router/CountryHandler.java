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
import org.walrex.application.dto.query.CountryFilter;
import org.walrex.application.dto.query.PageRequest;
import org.walrex.application.dto.request.CreateCountryRequest;
import org.walrex.application.dto.request.UpdateCountryRequest;
import org.walrex.application.dto.response.CountryResponse;
import org.walrex.application.port.input.*;
import org.walrex.domain.model.Country;
import org.walrex.infrastructure.adapter.inbound.mapper.CountryDtoMapper;
import org.walrex.infrastructure.adapter.inbound.mapper.CountryRequestMapper;

import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class CountryHandler {

    @Inject
    Validator validator;

    @Inject
    CountryRequestMapper countryRequestMapper;

    @Inject
    CountryDtoMapper countryDtoMapper;

    @Inject
    CreateCountryUseCase createCountryUseCase;

    @Inject
    ListCountryUseCase listCountryUseCase;

    @Inject
    GetCountryUseCase getCountryUseCase;

    @Inject
    UpdateCountryUseCase updateCountryUseCase;

    @Inject
    DeleteCountryUseCase deleteCountryUseCase;

    @Inject
    CheckAvailabilityCountryUseCase checkAvailabilityCountryUseCase;

    /**
     * POST /api/v1/countries - Create new country
     */
    @WithTransaction
    public Uni<Void> create(RoutingContext rc){
        try {
            CreateCountryRequest request = rc.body().asPojo(CreateCountryRequest.class);

            if(!validateCreateCountryRequest(rc, request)){
                return Uni.createFrom().voidItem();
            }

            Country country = countryRequestMapper.toModel(request);
            return createCountryUseCase.agregar(country)
                    .onItem().invoke(res->{
                        CountryResponse response = countryDtoMapper.toResponse(res);

                        String location = rc.request().absoluteURI() + "/" + response.id();
                        rc.response()
                                .setStatusCode(HttpResponseStatus.CREATED.code())
                                .putHeader("Content-Type", "application/json")
                                .putHeader("Location", location)
                                .end(Json.encode(response));
                    })
                    .onFailure().invoke(error -> handleError(rc, error))
                    .replaceWithVoid();
        }catch(Exception e){
            handleBadRequest(rc, "Invalid request body:" + e.getMessage());
            return Uni.createFrom().voidItem();
        }
    }


    /**
     * GET /api/v1/countries - List all countries with pagination and filtering
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
            String includeDeleted = rc.queryParams().get("includeDeleted");
            String search = rc.queryParams().get("search");

            // Build filter using builder
            CountryFilter filter = CountryFilter.builder()
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
            return listCountryUseCase.listar(pageRequest, filter)
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
     * GET /api/v1/countries/{id} - Get a country by ID
     */
    @WithSession
    public Uni<Void> getById(RoutingContext rc) {
        try {
            String idParam = rc.pathParam("id");
            Integer id = Integer.parseInt(idParam);

            return getCountryUseCase.findById(id)
                    .onItem().invoke(country -> {
                        CountryResponse response = countryDtoMapper.toResponse(country);
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
     * PUT /api/v1/countries/{id} - Update a country
     */
    @WithTransaction
    public Uni<Void> update(RoutingContext rc) {
        try {
            String idParam = rc.pathParam("id");
            Integer id = Integer.parseInt(idParam);

            UpdateCountryRequest request = rc.body().asPojo(UpdateCountryRequest.class);

            // Validate the request
            if (!validateUpdateCountryRequest(rc, request)) {
                return Uni.createFrom().voidItem(); // Validation failed, error response already sent
            }

            // Map request to domain model
            Country country = countryRequestMapper.toModel(request);

            return updateCountryUseCase.execute(id, country)
                    .onItem().invoke(updatedCountry -> {
                        CountryResponse response = countryDtoMapper.toResponse(updatedCountry);
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

            return deleteCountryUseCase.deshabilitar(id)
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
     * GET /api/v1/countries/check-availability - Check if a field value is
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
            checkUni = checkAvailabilityCountryUseCase.checkAlphabeticCode(alphabeticCode.trim().toUpperCase(),
                    finalExcludeId);
        } else if (!isBlank(numericCode)) {
            checkUni = checkAvailabilityCountryUseCase.checkNumericCode(Integer.parseInt(numericCode), finalExcludeId);
        } else {
            checkUni = checkAvailabilityCountryUseCase.checkName(name.trim(), finalExcludeId);
        }

        return checkUni
                .onItem().invoke(available -> sendJson(rc, HttpResponseStatus.OK, available))
                .onFailure().invoke(error -> handleError(rc, error))
                .replaceWithVoid();
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

    private void sendJson(RoutingContext rc, HttpResponseStatus status, Object body) {
        rc.response()
                .setStatusCode(status.code())
                .putHeader("Content-Type", "application/json")
                .end(Json.encode(body));
    }

    private void handleError(RoutingContext rc, Throwable error){
        String errorName = error.getClass().getSimpleName();

        HttpResponseStatus status;
        if(errorName.contains("NotFound")){
            status = HttpResponseStatus.NOT_FOUND;
        }else if(errorName.contains("Duplicate") || errorName.contains("Conflict")){
            status = HttpResponseStatus.CONFLICT;
        }else if(errorName.contains("Invalid") || errorName.contains("IllegalArgument")){
            status = HttpResponseStatus.BAD_REQUEST;
        }else{
            status = HttpResponseStatus.INTERNAL_SERVER_ERROR;
        }

        sendErrorResponse(rc, status, error.getMessage());
    }

    private boolean validateCreateCountryRequest(RoutingContext rc, CreateCountryRequest request){
        Set<ConstraintViolation<CreateCountryRequest>> violations = validator.validate(request);

        if(!violations.isEmpty()){
            String errorMessage = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining(", "));

            handleValidationError(rc, errorMessage);
            return false;
        }
        return true;
    }

    /**
     * Validates an UpdateCountryRequest using Jakarta Bean Validation.
     *
     * @param rc      The routing context for sending error responses
     * @param request The request object to validate
     * @return true if validation passes, false otherwise
     */
    private boolean validateUpdateCountryRequest(RoutingContext rc, UpdateCountryRequest request) {
        Set<ConstraintViolation<UpdateCountryRequest>> violations = validator.validate(request);

        if (!violations.isEmpty()) {
            String errorMessage = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining(", "));

            handleValidationError(rc, errorMessage);
            return false;
        }

        return true;
    }

    private void handleValidationError(RoutingContext rc, String validationErrors){
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

    private void handleBadRequest(RoutingContext rc, String messsage){
        sendErrorResponse(rc, HttpResponseStatus.BAD_REQUEST, messsage);
    }

    private void sendErrorResponse(RoutingContext rc, HttpResponseStatus status, String message){
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

    private boolean isBlank(String str) {
        return str == null || str.isBlank();
    }
}
