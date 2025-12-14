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
import org.walrex.application.dto.query.SystemDocumentTypeFilter;
import org.walrex.application.dto.request.CreateSystemDocumentTypeRequest;
import org.walrex.application.dto.request.UpdateSystemDocumentTypeRequest;
import org.walrex.application.dto.response.SystemDocumentTypeResponse;
import org.walrex.application.port.input.*;
import org.walrex.domain.model.SystemDocumentType;
import org.walrex.infrastructure.adapter.inbound.mapper.SystemDocumentTypeDtoMapper;
import org.walrex.infrastructure.adapter.inbound.mapper.SystemDocumentTypeRequestMapper;

import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class SystemDocumentTypeHandler {

    @Inject
    Validator validator;

    @Inject
    SystemDocumentTypeRequestMapper systemDocumentTypeRequestMapper;

    @Inject
    SystemDocumentTypeDtoMapper systemDocumentTypeDtoMapper;

    @Inject
    CreateSystemDocumentTypeUseCase createSystemDocumentTypeUseCase;

    @Inject
    ListSystemDocumentTypeUseCase listSystemDocumentTypeUseCase;

    @Inject
    GetSystemDocumentTypeUseCase getSystemDocumentTypeUseCase;

    @Inject
    UpdateSystemDocumentTypeUseCase updateSystemDocumentTypeUseCase;

    @Inject
    DeleteSystemDocumentTypeUseCase deleteSystemDocumentTypeUseCase;

    @Inject
    CheckAvailabilitySystemDocumentTypeUseCase checkAvailabilitySystemDocumentTypeUseCase;

    /**
     * POST /api/v1/system-document-types - Create new system document type
     */
    @WithTransaction
    public Uni<Void> create(RoutingContext rc) {
        try {
            CreateSystemDocumentTypeRequest request = rc.body().asPojo(CreateSystemDocumentTypeRequest.class);

            if (!validateCreateRequest(rc, request)) {
                return Uni.createFrom().voidItem();
            }

            SystemDocumentType systemDocumentType = systemDocumentTypeRequestMapper.toModel(request);
            return createSystemDocumentTypeUseCase.agregar(systemDocumentType)
                    .onItem().invoke(res -> {
                        SystemDocumentTypeResponse response = systemDocumentTypeDtoMapper.toResponse(res);

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
     * GET /api/v1/system-document-types - List all system document types with
     * pagination and filtering
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
            String code = rc.queryParams().get("code");
            String isRequiredParam = rc.queryParams().get("isRequired");
            String forPersonParam = rc.queryParams().get("forPerson");
            String forCompanyParam = rc.queryParams().get("forCompany");
            String activeParam = rc.queryParams().get("active");

            // Build filter using builder
            SystemDocumentTypeFilter.SystemDocumentTypeFilterBuilder filterBuilder = SystemDocumentTypeFilter.builder()
                    .search(search)
                    .code(code)
                    .includeDeleted(includeDeleted);

            if (isRequiredParam != null && !isRequiredParam.isBlank()) {
                filterBuilder.isRequired(Boolean.parseBoolean(isRequiredParam));
            }
            if (forPersonParam != null && !forPersonParam.isBlank()) {
                filterBuilder.forPerson(Boolean.parseBoolean(forPersonParam));
            }
            if (forCompanyParam != null && !forCompanyParam.isBlank()) {
                filterBuilder.forCompany(Boolean.parseBoolean(forCompanyParam));
            }
            if (activeParam != null && !activeParam.isBlank()) {
                filterBuilder.active(Boolean.parseBoolean(activeParam));
            }

            SystemDocumentTypeFilter filter = filterBuilder.build();

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
            return listSystemDocumentTypeUseCase.listar(pageRequest, filter)
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
     * GET /api/v1/system-document-types/{id} - Get a system document type by ID
     */
    @WithSession
    public Uni<Void> getById(RoutingContext rc) {
        try {
            String idParam = rc.pathParam("id");
            Long id = Long.parseLong(idParam);

            return getSystemDocumentTypeUseCase.findById(id)
                    .onItem().invoke(systemDocumentType -> {
                        SystemDocumentTypeResponse response = systemDocumentTypeDtoMapper
                                .toResponse(systemDocumentType);
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
     * PUT /api/v1/system-document-types/{id} - Update a system document type
     */
    @WithTransaction
    public Uni<Void> update(RoutingContext rc) {
        try {
            String idParam = rc.pathParam("id");
            Long id = Long.parseLong(idParam);

            UpdateSystemDocumentTypeRequest request = rc.body().asPojo(UpdateSystemDocumentTypeRequest.class);

            // Validate the request
            if (!validateUpdateRequest(rc, request)) {
                return Uni.createFrom().voidItem(); // Validation failed, error response already sent
            }

            // Map request to domain model
            SystemDocumentType systemDocumentType = systemDocumentTypeRequestMapper.toModel(request);

            return updateSystemDocumentTypeUseCase.execute(id, systemDocumentType)
                    .onItem().invoke(updatedDocumentType -> {
                        SystemDocumentTypeResponse response = systemDocumentTypeDtoMapper
                                .toResponse(updatedDocumentType);
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
     * DELETE /api/v1/system-document-types/{id} - Delete a system document type
     * (soft delete)
     */
    @WithTransaction
    public Uni<Void> delete(RoutingContext rc) {
        try {
            String idParam = rc.pathParam("id");
            Long id = Long.parseLong(idParam);

            return deleteSystemDocumentTypeUseCase.deshabilitar(id)
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
     * GET /api/v1/system-document-types/check-availability - Check if a field value
     * is available
     */
    @WithSession
    public Uni<Void> checkAvailability(RoutingContext rc) {
        String code = rc.queryParams().get("code");
        String name = rc.queryParams().get("name");
        String excludeIdParam = rc.queryParams().get("excludeId");

        // Parse excludeId if provided
        Long excludeId = null;
        if (excludeIdParam != null && !excludeIdParam.isBlank()) {
            try {
                excludeId = Long.parseLong(excludeIdParam);
            } catch (NumberFormatException e) {
                handleBadRequest(rc, "Invalid excludeId format. It must be a number.");
                return Uni.createFrom().voidItem();
            }
        }

        // Validate that at least one parameter is provided
        if (isBlank(code) && isBlank(name)) {
            handleBadRequest(rc, "At least one query parameter is required: code or name");
            return Uni.createFrom().voidItem();
        }

        // Check which field to validate (priority: code > name)
        final Long finalExcludeId = excludeId;
        Uni<org.walrex.application.dto.response.AvailabilityResponse> checkUni;

        if (!isBlank(code)) {
            checkUni = checkAvailabilitySystemDocumentTypeUseCase.checkCode(code.trim().toUpperCase(), finalExcludeId);
        } else {
            checkUni = checkAvailabilitySystemDocumentTypeUseCase.checkName(name.trim(), finalExcludeId);
        }

        return checkUni
                .onItem().invoke(available -> sendJson(rc, HttpResponseStatus.OK, available))
                .onFailure().invoke(error -> handleError(rc, error))
                .replaceWithVoid();
    }

    // ==================== Helper Methods ====================

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

    private boolean validateCreateRequest(RoutingContext rc, CreateSystemDocumentTypeRequest request) {
        Set<ConstraintViolation<CreateSystemDocumentTypeRequest>> violations = validator.validate(request);

        if (!violations.isEmpty()) {
            String errorMessage = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining(", "));

            handleValidationError(rc, errorMessage);
            return false;
        }
        return true;
    }

    private boolean validateUpdateRequest(RoutingContext rc, UpdateSystemDocumentTypeRequest request) {
        Set<ConstraintViolation<UpdateSystemDocumentTypeRequest>> violations = validator.validate(request);

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
