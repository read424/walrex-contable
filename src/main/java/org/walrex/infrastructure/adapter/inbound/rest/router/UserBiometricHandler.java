package org.walrex.infrastructure.adapter.inbound.rest.router;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.walrex.application.dto.request.BiometricRequest;
import org.walrex.application.dto.response.BiometricResponse;
import org.walrex.application.port.input.UpdateUserBiometricUseCase;
import org.walrex.infrastructure.adapter.inbound.rest.security.JwtSecurityInterceptor;

import java.util.Optional;

@Slf4j
@ApplicationScoped
public class UserBiometricHandler {

    @Inject
    Validator validator;

    @Inject
    UpdateUserBiometricUseCase updateUserBiometricUseCase;

    @Inject
    JwtSecurityInterceptor jwtSecurityInterceptor;

    public Uni<Void> updateBiometric(RoutingContext rc) {
        try {
            // Autenticación via interceptor
            Optional<JsonWebToken> tokenOpt = jwtSecurityInterceptor.authenticate(rc);
            if (tokenOpt.isEmpty()) {
                return Uni.createFrom().voidItem();
            }

            Integer userId = jwtSecurityInterceptor.getUserId(tokenOpt.get());

            BiometricRequest request = rc.body().asPojo(BiometricRequest.class);
            // validate
            var violations = validator.validate(request);
            if (!violations.isEmpty()) {
                String errors = violations.stream().map(v -> v.getPropertyPath() + ": " + v.getMessage()).reduce((a,b)->a+", "+b).orElse("");
                return sendBadRequest(rc, "Validation failed: " + errors);
            }

            return updateUserBiometricUseCase.updateBiometric(userId, request.getEnabled(), request.getBiometricType())
                    .onItem().transformToUni(user -> sendSuccess(rc, user))
                    .onFailure().recoverWithUni(failure -> sendErrorResponse(rc, failure));

        } catch (Exception e) {
            log.error("Unexpected error in biometric handler", e);
            return sendExceptionResponse(rc, e);
        }
    }

    private Uni<Void> sendSuccess(RoutingContext rc, org.walrex.domain.model.User user){
        boolean enabled = Boolean.TRUE.equals(user.getBiometricEnabled());
        String message = enabled
                ? "Autenticación biométrica activada correctamente"
                : "Autenticación biométrica desactivada correctamente";

        BiometricResponse resp = BiometricResponse.builder()
                .success(true)
                .message(message)
                .biometricEnabled(enabled)
                .build();
        rc.response()
                .setStatusCode(HttpResponseStatus.OK.code())
                .putHeader("content-type", "application/json")
                .end(JsonObject.mapFrom(resp).encode());
        return Uni.createFrom().voidItem();
    }

    private Uni<Void> sendBadRequest(RoutingContext rc, String message){
        JsonObject error = new JsonObject()
                .put("success", false)
                .put("error", "Bad Request")
                .put("message", message)
                .put("status", HttpResponseStatus.BAD_REQUEST.code());
        rc.response()
                .setStatusCode(HttpResponseStatus.BAD_REQUEST.code())
                .putHeader("content-type", "application/json")
                .end(error.encode());
        return Uni.createFrom().voidItem();
    }

    private Uni<Void> sendErrorResponse(RoutingContext rc, Throwable failure){
        int statusCode = HttpResponseStatus.INTERNAL_SERVER_ERROR.code();
        String message = failure.getMessage() != null ? failure.getMessage() : "Error al actualizar biometría";
        JsonObject error = new JsonObject()
                .put("success", false)
                .put("error", "Internal Server Error")
                .put("message", message)
                .put("status", statusCode);
        rc.response()
                .setStatusCode(statusCode)
                .putHeader("content-type", "application/json")
                .end(error.encode());
        return Uni.createFrom().voidItem();
    }

    private Uni<Void> sendExceptionResponse(RoutingContext rc, Exception e){
        JsonObject error = new JsonObject()
                .put("success", false)
                .put("error", "Internal server error")
                .put("message", "Error inesperado en el handler")
                .put("status", HttpResponseStatus.INTERNAL_SERVER_ERROR.code());
        rc.response()
                .setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                .putHeader("content-type", "application/json")
                .end(error.encode());
        return Uni.createFrom().voidItem();
    }
}
