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
import org.walrex.application.dto.request.AssignCurrencyRequest;
import org.walrex.application.port.input.ManageCountryCurrencyUseCase;

import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class CountryCurrencyHandler {

    @Inject
    Validator validator;

    @Inject
    ManageCountryCurrencyUseCase manageCountryCurrencyUseCase;

    /**
     * GET /{countryId}/currencies - Listar monedas del país
     */
    @WithSession
    public Uni<Void> listCurrencies(RoutingContext rc) {
        try {
            String countryIdParam = rc.pathParam("countryId");
            Integer countryId = Integer.parseInt(countryIdParam);

            return manageCountryCurrencyUseCase.listCurrenciesByCountry(countryId)
                    .onItem().invoke(currencies -> sendJson(rc, HttpResponseStatus.OK, currencies))
                    .onFailure().invoke(error -> handleError(rc, error))
                    .replaceWithVoid();
        } catch (NumberFormatException e) {
            handleBadRequest(rc, "Invalid country ID format. ID must be a number.");
            return Uni.createFrom().voidItem();
        }
    }

    /**
     * POST /{countryId}/currencies/{currencyId} - Asignar moneda
     */
    @WithTransaction
    public Uni<Void> assignCurrency(RoutingContext rc) {
        try {
            String countryIdParam = rc.pathParam("countryId");
            String currencyIdParam = rc.pathParam("currencyId");

            Integer countryId = Integer.parseInt(countryIdParam);
            Integer currencyId = Integer.parseInt(currencyIdParam);

            return manageCountryCurrencyUseCase.assignCurrency(countryId, currencyId)
                    .onItem().invoke(response -> {
                        String location = rc.request().absoluteURI();
                        rc.response()
                                .setStatusCode(HttpResponseStatus.CREATED.code())
                                .putHeader("Content-Type", "application/json")
                                .putHeader("Location", location)
                                .end(Json.encode(response));
                    })
                    .onFailure().invoke(error -> handleError(rc, error))
                    .replaceWithVoid();
        } catch (NumberFormatException e) {
            handleBadRequest(rc, "Invalid ID format. IDs must be numbers.");
            return Uni.createFrom().voidItem();
        } catch (Exception e) {
            handleBadRequest(rc, "Invalid request: " + e.getMessage());
            return Uni.createFrom().voidItem();
        }
    }

    /**
     * PUT /{countryId}/currencies/{currencyId}/default - Establecer moneda como predeterminada
     */
    @WithTransaction
    public Uni<Void> setDefaultCurrency(RoutingContext rc) {
        try {
            String countryIdParam = rc.pathParam("countryId");
            String currencyIdParam = rc.pathParam("currencyId");

            Integer countryId = Integer.parseInt(countryIdParam);
            Integer currencyId = Integer.parseInt(currencyIdParam);

            return manageCountryCurrencyUseCase.setDefaultCurrency(countryId, currencyId)
                    .onItem().invoke(response -> sendJson(rc, HttpResponseStatus.OK, response))
                    .onFailure().invoke(error -> handleError(rc, error))
                    .replaceWithVoid();
        } catch (NumberFormatException e) {
            handleBadRequest(rc, "Invalid ID format. IDs must be numbers.");
            return Uni.createFrom().voidItem();
        }
    }

    /**
     * PUT /{countryId}/currencies/{currencyId}/operation - Activar/Desactivar moneda
     */
    @WithTransaction
    public Uni<Void> updateOperationalStatus(RoutingContext rc) {
        try {
            String countryIdParam = rc.pathParam("countryId");
            String currencyIdParam = rc.pathParam("currencyId");

            Integer countryId = Integer.parseInt(countryIdParam);
            Integer currencyId = Integer.parseInt(currencyIdParam);

            // Leer el body para obtener el estado isOperational
            JsonObject body = rc.body().asJsonObject();
            Boolean isOperational = body.getBoolean("isOperational");

            if (isOperational == null) {
                handleBadRequest(rc, "isOperational field is required in request body");
                return Uni.createFrom().voidItem();
            }

            return manageCountryCurrencyUseCase.updateOperationalStatus(countryId, currencyId, isOperational)
                    .onItem().invoke(response -> sendJson(rc, HttpResponseStatus.OK, response))
                    .onFailure().invoke(error -> handleError(rc, error))
                    .replaceWithVoid();
        } catch (NumberFormatException e) {
            handleBadRequest(rc, "Invalid ID format. IDs must be numbers.");
            return Uni.createFrom().voidItem();
        } catch (Exception e) {
            handleBadRequest(rc, "Invalid request body: " + e.getMessage());
            return Uni.createFrom().voidItem();
        }
    }

    /**
     * GET /{countryId}/default-currency - Obtener moneda predeterminada
     */
    @WithSession
    public Uni<Void> getDefaultCurrency(RoutingContext rc) {
        try {
            String countryIdParam = rc.pathParam("countryId");
            Integer countryId = Integer.parseInt(countryIdParam);

            return manageCountryCurrencyUseCase.getDefaultCurrency(countryId)
                    .onItem().invoke(currency -> sendJson(rc, HttpResponseStatus.OK, currency))
                    .onFailure().invoke(error -> handleError(rc, error))
                    .replaceWithVoid();
        } catch (NumberFormatException e) {
            handleBadRequest(rc, "Invalid country ID format. ID must be a number.");
            return Uni.createFrom().voidItem();
        }
    }

    // Helper methods

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
        } else if (errorName.contains("Duplicate") || errorName.contains("Conflict") ||
                errorName.contains("already assigned")) {
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
}