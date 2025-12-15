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
import org.walrex.application.dto.query.PageRequest;
import org.walrex.application.dto.query.SunatDocumentTypeFilter;
import org.walrex.application.dto.request.CreateSunatDocumentTypeRequest;
import org.walrex.application.dto.request.UpdateSunatDocumentTypeRequest;
import org.walrex.application.dto.response.SunatDocumentTypeResponse;
import org.walrex.application.port.input.*;
import org.walrex.domain.model.SunatDocumentType;
import org.walrex.infrastructure.adapter.inbound.mapper.SunatDocumentTypeDtoMapper;
import org.walrex.infrastructure.adapter.inbound.mapper.SunatDocumentTypeRequestMapper;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Handler HTTP para tipos de documentos SUNAT.
 *
 * Responsabilidades:
 * - Validación de DTOs con Jakarta Bean Validation
 * - Manejo del RoutingContext (request/response)
 * - Mapeo entre DTOs y modelo de dominio
 * - Invocación de casos de uso
 * - Manejo de errores HTTP
 */
@ApplicationScoped
public class SunatDocumentTypeHandler {

    @Inject
    Validator validator;

    @Inject
    SunatDocumentTypeRequestMapper requestMapper;

    @Inject
    SunatDocumentTypeDtoMapper dtoMapper;

    @Inject
    CreateSunatDocumentTypeUseCase createUseCase;

    @Inject
    ListSunatDocumentTypeUseCase listUseCase;

    @Inject
    GetSunatDocumentTypeUseCase getUseCase;

    @Inject
    UpdateSunatDocumentTypeUseCase updateUseCase;

    @Inject
    DeleteSunatDocumentTypeUseCase deleteUseCase;

    @Inject
    CheckAvailabilitySunatDocumentTypeUseCase checkAvailabilityUseCase;

    /**
     * POST /api/v1/sunat-document-types - Create new document type
     */
    @WithTransaction
    public Uni<Void> create(RoutingContext rc) {
        try {
            CreateSunatDocumentTypeRequest request = rc.body().asPojo(CreateSunatDocumentTypeRequest.class);

            if (!validateCreateRequest(rc, request)) {
                return Uni.createFrom().voidItem();
            }

            SunatDocumentType documentType = requestMapper.toModel(request);
            return createUseCase.create(documentType)
                    .onItem().invoke(res -> {
                        SunatDocumentTypeResponse response = dtoMapper.toResponse(res);

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
     * GET /api/v1/sunat-document-types - List all document types with pagination and filtering
     */
    @WithSession
    public Uni<Void> list(RoutingContext rc) {
        try {
            // Parse query parameters
            // Frontend sends 1-based pages (page 1 is first), convert to 0-based for backend
            int page = Math.max(0, getQueryParamAsInt(rc, "page", 1) - 1);
            int size = getQueryParamAsInt(rc, "size", 10);
            String sortBy = rc.queryParams().get("sortBy");
            String sortDirection = rc.queryParams().get("sortDirection");
            String includeInactive = rc.queryParams().get("includeInactive");
            String search = rc.queryParams().get("search");
            String code = rc.queryParams().get("code");
            Boolean active = getBooleanQueryParam(rc, "active");
            String length_str = rc.queryParams().get("length");
            Integer length = null;
            if(length_str!=null) {
                length = getQueryParamAsInt(rc, "length", null);
            }

            // Build filter using builder
            SunatDocumentTypeFilter filter = SunatDocumentTypeFilter.builder()
                    .search(search)
                    .code(code)
                    .active(active)
                    .length(length)
                    .includeInactive(includeInactive)
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
            return listUseCase.list(pageRequest, filter)
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
     * GET /api/v1/sunat-document-types/{id} - Get a document type by ID
     */
    @WithSession
    public Uni<Void> getById(RoutingContext rc) {
        try {
            String id = rc.pathParam("id");

            return getUseCase.findById(Integer.parseInt(id))
                    .onItem().invoke(documentType -> {
                        SunatDocumentTypeResponse response = dtoMapper.toResponse(documentType);
                        sendJson(rc, HttpResponseStatus.OK, response);
                    })
                    .onFailure().invoke(error -> handleError(rc, error))
                    .replaceWithVoid();
        } catch (Exception e) {
            handleBadRequest(rc, "Invalid ID format: " + e.getMessage());
            return Uni.createFrom().voidItem();
        }
    }

    /**
     * PUT /api/v1/sunat-document-types/{id} - Update a document type
     */
    @WithTransaction
    public Uni<Void> update(RoutingContext rc) {
        try {
            String id = rc.pathParam("id");

            UpdateSunatDocumentTypeRequest request = rc.body().asPojo(UpdateSunatDocumentTypeRequest.class);

            // Validate the request
            if (!validateUpdateRequest(rc, request)) {
                return Uni.createFrom().voidItem(); // Validation failed, error response already sent
            }

            // Map request to domain model
            SunatDocumentType documentType = requestMapper.toModel(request);

            return updateUseCase.update(Integer.parseInt(id), documentType)
                    .onItem().invoke(updatedDocumentType -> {
                        SunatDocumentTypeResponse response = dtoMapper.toResponse(updatedDocumentType);
                        sendJson(rc, HttpResponseStatus.OK, response);
                    })
                    .onFailure().invoke(error -> handleError(rc, error))
                    .replaceWithVoid();
        } catch (Exception e) {
            handleBadRequest(rc, "Invalid request body: " + e.getMessage());
            return Uni.createFrom().voidItem();
        }
    }

    /**
     * DELETE /api/v1/sunat-document-types/{id} - Deactivate a document type
     */
    @WithTransaction
    public Uni<Void> delete(RoutingContext rc) {
        try {
            String id = rc.pathParam("id");

            return deleteUseCase.deactivate(Integer.parseInt(id))
                    .onItem().invoke(voidItem -> {
                        rc.response()
                                .setStatusCode(HttpResponseStatus.NO_CONTENT.code())
                                .end();
                    })
                    .onFailure().invoke(error -> handleError(rc, error))
                    .replaceWithVoid();
        } catch (Exception e) {
            handleBadRequest(rc, "Invalid ID format: " + e.getMessage());
            return Uni.createFrom().voidItem();
        }
    }

    /**
     * GET /api/v1/sunat-document-types/check-availability - Check if a field value is available
     * Query params: id, code
     * Optional: excludeId (to exclude a specific ID from the check, useful for updates)
     */
    @WithSession
    public Uni<Void> checkAvailability(RoutingContext rc) {
        String id = rc.queryParams().get("id");
        String code = rc.queryParams().get("code");
        String excludeId = rc.queryParams().get("excludeId");

        // Validate that at least one parameter is provided
        if (isBlank(id) && isBlank(code)) {
            handleBadRequest(rc, "At least one query parameter is required: id or code");
            return Uni.createFrom().voidItem();
        }

        // Check which field to validate (priority: id > code)
        Uni<org.walrex.application.dto.response.AvailabilityResponse> checkUni;

        if (id!=null) {
            checkUni = checkAvailabilityUseCase.checkId(Integer.parseInt(id), excludeId);
        } else {
            checkUni = checkAvailabilityUseCase.checkCode(code.trim().toUpperCase(), excludeId);
        }

        return checkUni
                .onItem().invoke(available -> sendJson(rc, HttpResponseStatus.OK, available))
                .onFailure().invoke(error -> handleError(rc, error))
                .replaceWithVoid();
    }

    // ==================== Helper Methods ====================

    private int getQueryParamAsInt(RoutingContext rc, String name, Integer defaultValue) {
        String value = rc.queryParams().get(name);
        if (value == null || value.isBlank()) {
            return defaultValue != null ? defaultValue : 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue != null ? defaultValue : 0;
        }
    }

    private Boolean getBooleanQueryParam(RoutingContext rc, String name) {
        String value = rc.queryParams().get(name);
        if (value == null || value.isBlank()) {
            return null;
        }
        return Boolean.parseBoolean(value);
    }

    private void sendJson(RoutingContext rc, HttpResponseStatus status, Object body) {
        rc.response()
                .setStatusCode(status.code())
                .putHeader("Content-Type", "application/json")
                .end(Json.encode(body));
    }

    private void handleError(RoutingContext rc, Throwable error) {
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

    private boolean validateCreateRequest(RoutingContext rc, CreateSunatDocumentTypeRequest request) {
        Set<ConstraintViolation<CreateSunatDocumentTypeRequest>> violations = validator.validate(request);

        if (!violations.isEmpty()) {
            String errorMessage = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining(", "));

            handleValidationError(rc, errorMessage);
            return false;
        }
        return true;
    }

    private boolean validateUpdateRequest(RoutingContext rc, UpdateSunatDocumentTypeRequest request) {
        Set<ConstraintViolation<UpdateSunatDocumentTypeRequest>> violations = validator.validate(request);

        if (!violations.isEmpty()) {
            String errorMessage = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining(", "));

            handleValidationError(rc, errorMessage);
            return false;
        }

        return true;
    }

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

    private boolean isBlank(String str) {
        return str == null || str.isBlank();
    }
}
