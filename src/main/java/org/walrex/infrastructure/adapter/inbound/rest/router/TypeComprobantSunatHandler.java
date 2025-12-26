package org.walrex.infrastructure.adapter.inbound.rest.router;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.walrex.application.port.input.GetAllTypeComprobantsSunatUseCase;

@Slf4j
@ApplicationScoped
public class TypeComprobantSunatHandler {

    @Inject
    GetAllTypeComprobantsSunatUseCase getAllTypeComprobantsSunatUseCase;

    /**
     * GET /api/v1/typeComprobantsSunat/all - Get all type comprobants SUNAT
     */
    @WithSession
    public Uni<Void> getAll(RoutingContext rc) {
        log.info("Handling GET /api/v1/typeComprobantsSunat/all request");

        return getAllTypeComprobantsSunatUseCase.execute()
                .onItem().invoke(response -> {
                    sendJson(rc, HttpResponseStatus.OK, response);
                })
                .onFailure().invoke(error -> {
                    log.error("Error getting all type comprobants SUNAT", error);
                    handleError(rc, error);
                })
                .replaceWithVoid();
    }

    // ==================== Helper Methods ====================

    private void sendJson(RoutingContext rc, HttpResponseStatus status, Object body) {
        rc.response()
                .setStatusCode(status.code())
                .putHeader("Content-Type", "application/json")
                .end(Json.encode(body));
    }

    private void handleError(RoutingContext rc, Throwable error) {
        HttpResponseStatus status = HttpResponseStatus.INTERNAL_SERVER_ERROR;
        String errorName = error.getClass().getSimpleName();

        if (errorName.contains("NotFound")) {
            status = HttpResponseStatus.NOT_FOUND;
        } else if (errorName.contains("Invalid") || errorName.contains("IllegalArgument")) {
            status = HttpResponseStatus.BAD_REQUEST;
        }

        sendErrorResponse(rc, status, error.getMessage());
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
