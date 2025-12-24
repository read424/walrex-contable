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
import lombok.extern.slf4j.Slf4j;
import org.walrex.application.dto.query.AccountingAccountFilter;
import org.walrex.application.dto.query.PageRequest;
import org.walrex.application.dto.request.CreateAccountingAccountRequest;
import org.walrex.application.dto.request.UpdateAccountingAccountRequest;
import org.walrex.application.dto.response.AccountingAccountResponse;
import org.walrex.application.port.input.*;
import org.walrex.domain.exception.AccountingAccountNotFoundException;
import org.walrex.domain.exception.DuplicateAccountingAccountException;
import org.walrex.domain.model.AccountingAccount;
import org.walrex.infrastructure.adapter.inbound.mapper.AccountingAccountDtoMapper;
import org.walrex.infrastructure.adapter.inbound.mapper.AccountingAccountRequestMapper;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Handler para procesar peticiones HTTP de cuentas contables.
 *
 * Responsabilidades:
 * - Parsear parámetros y cuerpo de peticiones HTTP
 * - Validar DTOs de entrada
 * - Invocar casos de uso
 * - Mapear respuestas y errores a formato HTTP/JSON
 */
@Slf4j
@ApplicationScoped
public class AccountingAccountHandler {

    @Inject
    CreateAccountingAccountUseCase createAccountUseCase;

    @Inject
    GetAccountingAccountUseCase getAccountUseCase;

    @Inject
    ListAccountingAccountsUseCase listAccountsUseCase;

    @Inject
    UpdateAccountingAccountUseCase updateAccountUseCase;

    @Inject
    DeleteAccountingAccountUseCase deleteAccountUseCase;

    @Inject
    Validator validator;

    @Inject
    AccountingAccountRequestMapper accountRequestMapper;

    @Inject
    AccountingAccountDtoMapper accountDtoMapper;

    /**
     * POST /api/v1/accountingAccounts - Create a new account
     */
    @WithTransaction
    public Uni<Void> create(RoutingContext rc) {
        try {
            CreateAccountingAccountRequest request = rc.body().asPojo(CreateAccountingAccountRequest.class);

            // Validate the request
            if (!validateRequest(rc, request)) {
                return Uni.createFrom().voidItem();
            }

            // Map request to domain model
            AccountingAccount accountingAccount = accountRequestMapper.toModel(request);

            return createAccountUseCase.execute(accountingAccount)
                    .onItem().invoke(accountDomain -> {
                        AccountingAccountResponse response = accountDtoMapper.toResponse(accountDomain);
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
     * GET /api/v1/accountingAccounts - List all accountingAccounts with pagination and filtering
     */
    @WithSession
    public Uni<Void> list(RoutingContext rc) {
        try {
            // Parse query parameters
            // Frontend sends 1-based pages (page 1 is first), convert to 0-based for backend
            int page = Math.max(0, getQueryParamAsInt(rc, "page", 1) - 1);
            int size = getQueryParamAsInt(rc, "size", 20);
            String sortBy = rc.queryParams().get("sortBy");
            String sortDirection = rc.queryParams().get("sortDirection");
            String search = rc.queryParams().get("search");
            String type = rc.queryParams().get("type");
            String normalSide = rc.queryParams().get("normalSide");
            String active = rc.queryParams().get("active");

            // Build filter
            AccountingAccountFilter filter = AccountingAccountFilter.builder()
                    .search(search)
                    .type(type)
                    .normalSide(normalSide)
                    .active(active)
                    .includeDeleted("0")
                    .build();

            // Build page request
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
            return listAccountsUseCase.execute(pageRequest, filter)
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
     * GET /api/v1/accountingAccounts/all - List all accountingAccounts without pagination
     */
    @WithSession
    public Uni<Void> findAll(RoutingContext rc) {
        try {
            // Parse query parameters
            String search = rc.queryParams().get("search");
            String type = rc.queryParams().get("type");
            String active = rc.queryParams().get("active");

            // Build filter (por defecto solo cuentas activas)
            AccountingAccountFilter filter = AccountingAccountFilter.builder()
                    .search(search)
                    .type(type)
                    .active(active != null ? active : "1")
                    .build();

            // Execute use case
            return listAccountsUseCase.findAll(filter)
                    .onItem().invoke(accountingAccounts -> {
                        sendJson(rc, HttpResponseStatus.OK, accountingAccounts);
                    })
                    .onFailure().invoke(error -> handleError(rc, error))
                    .replaceWithVoid();
        } catch (Exception e) {
            handleBadRequest(rc, "Invalid query parameters: " + e.getMessage());
            return Uni.createFrom().voidItem();
        }
    }

    /**
     * GET /api/v1/accountingAccounts/{id} - Get an accountingAccount by ID
     */
    @WithSession
    public Uni<Void> getById(RoutingContext rc) {
        try {
            String idParam = rc.pathParam("id");
            Integer id = Integer.parseInt(idParam);

            return getAccountUseCase.findById(id)
                    .onItem().invoke(accountingAccount -> {
                        AccountingAccountResponse response = accountDtoMapper.toResponse(accountingAccount);
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
     * PUT /api/v1/accountingAccounts/{id} - Update an account
     */
    @WithTransaction
    public Uni<Void> update(RoutingContext rc) {
        try {
            String idParam = rc.pathParam("id");
            Integer id = Integer.parseInt(idParam);

            UpdateAccountingAccountRequest request = rc.body().asPojo(UpdateAccountingAccountRequest.class);

            // Validate the request
            if (!validateRequest(rc, request)) {
                return Uni.createFrom().voidItem();
            }

            // Map request to domain model
            AccountingAccount accountingAccount = accountRequestMapper.toModel(request);

            return updateAccountUseCase.execute(id, accountingAccount)
                    .onItem().invoke(updatedAccountingAccount -> {
                        AccountingAccountResponse response = accountDtoMapper.toResponse(updatedAccountingAccount);
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
     * DELETE /api/v1/accountingAccounts/{id} - Delete an accountingAccount (soft delete)
     */
    @WithTransaction
    public Uni<Void> delete(RoutingContext rc) {
        try {
            String idParam = rc.pathParam("id");
            Integer id = Integer.parseInt(idParam);

            return deleteAccountUseCase.execute(id)
                    .onItem().invoke(deleted -> {
                        if (deleted) {
                            rc.response()
                                    .setStatusCode(HttpResponseStatus.NO_CONTENT.code())
                                    .end();
                        } else {
                            handleNotFound(rc, "AccountingAccount with id " + id + " not found");
                        }
                    })
                    .onFailure().invoke(error -> handleError(rc, error))
                    .replaceWithVoid();
        } catch (NumberFormatException e) {
            handleBadRequest(rc, "Invalid ID format. ID must be a number.");
            return Uni.createFrom().voidItem();
        }
    }

    /**
     * PUT /api/v1/accountingAccounts/{id}/restore - Restore a deleted account
     */
    @WithTransaction
    public Uni<Void> restore(RoutingContext rc) {
        try {
            String idParam = rc.pathParam("id");
            Integer id = Integer.parseInt(idParam);

            return deleteAccountUseCase.restore(id)
                    .onItem().invoke(restored -> {
                        if (restored) {
                            JsonObject response = new JsonObject()
                                    .put("message", "AccountingAccount restored successfully")
                                    .put("id", id);
                            sendJson(rc, HttpResponseStatus.OK, response);
                        } else {
                            handleNotFound(rc, "AccountingAccount with id " + id + " not found or was not deleted");
                        }
                    })
                    .onFailure().invoke(error -> handleError(rc, error))
                    .replaceWithVoid();
        } catch (NumberFormatException e) {
            handleBadRequest(rc, "Invalid ID format. ID must be a number.");
            return Uni.createFrom().voidItem();
        }
    }

    // ==================== Métodos de Utilidad ====================

    /**
     * Valida un request DTO usando Bean Validation.
     */
    private <T> boolean validateRequest(RoutingContext rc, T request) {
        Set<ConstraintViolation<T>> violations = validator.validate(request);

        if (!violations.isEmpty()) {
            String errors = violations.stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.joining(", "));

            handleBadRequest(rc, "Validation failed: " + errors);
            return false;
        }

        return true;
    }

    /**
     * Parsea un parámetro de query como entero.
     */
    private int getQueryParamAsInt(RoutingContext rc, String paramName, int defaultValue) {
        String param = rc.queryParams().get(paramName);
        if (param == null || param.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(param);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Envía una respuesta JSON.
     */
    private void sendJson(RoutingContext rc, HttpResponseStatus status, Object data) {
        rc.response()
                .setStatusCode(status.code())
                .putHeader("Content-Type", "application/json")
                .end(Json.encode(data));
    }

    /**
     * Maneja errores de validación (400 Bad Request).
     */
    private void handleBadRequest(RoutingContext rc, String message) {
        log.warn("Bad request: {}", message);
        JsonObject error = new JsonObject()
                .put("error", "Bad Request")
                .put("message", message)
                .put("status", HttpResponseStatus.BAD_REQUEST.code());

        rc.response()
                .setStatusCode(HttpResponseStatus.BAD_REQUEST.code())
                .putHeader("Content-Type", "application/json")
                .end(error.encode());
    }

    /**
     * Maneja errores de recurso no encontrado (404 Not Found).
     */
    private void handleNotFound(RoutingContext rc, String message) {
        log.warn("Not found: {}", message);
        JsonObject error = new JsonObject()
                .put("error", "Not Found")
                .put("message", message)
                .put("status", HttpResponseStatus.NOT_FOUND.code());

        rc.response()
                .setStatusCode(HttpResponseStatus.NOT_FOUND.code())
                .putHeader("Content-Type", "application/json")
                .end(error.encode());
    }

    /**
     * Maneja errores de conflicto (409 Conflict).
     */
    private void handleConflict(RoutingContext rc, String message) {
        log.warn("Conflict: {}", message);
        JsonObject error = new JsonObject()
                .put("error", "Conflict")
                .put("message", message)
                .put("status", HttpResponseStatus.CONFLICT.code());

        rc.response()
                .setStatusCode(HttpResponseStatus.CONFLICT.code())
                .putHeader("Content-Type", "application/json")
                .end(error.encode());
    }

    /**
     * Maneja errores genéricos mapeándolos a códigos HTTP apropiados.
     */
    private void handleError(RoutingContext rc, Throwable error) {
        log.error("Error handling request", error);

        if (error instanceof AccountingAccountNotFoundException) {
            handleNotFound(rc, error.getMessage());
        } else if (error instanceof DuplicateAccountingAccountException) {
            handleConflict(rc, error.getMessage());
        } else {
            JsonObject errorResponse = new JsonObject()
                    .put("error", "Internal Server Error")
                    .put("message", error.getMessage())
                    .put("status", HttpResponseStatus.INTERNAL_SERVER_ERROR.code());

            rc.response()
                    .setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                    .putHeader("Content-Type", "application/json")
                    .end(errorResponse.encode());
        }
    }
}
