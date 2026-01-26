package org.walrex.infrastructure.adapter.inbound.rest.router;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.walrex.application.dto.request.RegisterUserRequest;
import org.walrex.application.port.input.RegisterUserUseCase;
import org.walrex.domain.exception.DuplicateCustomerException;
import org.walrex.domain.exception.DuplicateUserException;
import org.walrex.domain.exception.InvalidTokenException;
import org.walrex.domain.model.RegisteredUser;

import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@ApplicationScoped
public class UserRegistrationHandler {
    @Inject
    Validator validator;

    @Inject
    RegisterUserUseCase registerUserUseCase;

    public Uni<Void> createdUser(RoutingContext rc){
        log.debug("Received user registration request from mobile app");
        try {
            // Log del body crudo recibido
            String rawBody = rc.body().asString();
            log.info("Raw request body received: {}", rawBody);

            RegisterUserRequest request = rc.body().asPojo(RegisterUserRequest.class);
            log.info("Deserialized RegisterUserRequest: {}", request);

            // PASO 2: Validar request con Bean Validation
            if (!validateRequest(rc, request)) {
                return Uni.createFrom().voidItem();
            }
            log.debug("Request validation passed");

            return registerUserUseCase.register(request)
                    .onItem().transformToUni(registeredUser -> {
                        log.info("User registered successfully: userId={}, clientid={}", registeredUser.getUserId(), registeredUser.getClientId());
                        return sendSuccessResponse(rc, registeredUser);
                    })
                    .onFailure().recoverWithUni(failure->{
                        log.error("Error registering user", failure);
                        return sendErrorResponse(rc, failure);
                    });
        }catch(Exception e){
            log.error("Unexpected error in user registration", e);
            return sendExceptionResponse(rc, e);
        }
    }

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
    * Envia respuesta exitosa (201 Created)
     */
    private Uni<Void> sendSuccessResponse(RoutingContext rc, RegisteredUser user){
        JsonObject response = new JsonObject()
                .put("success", true)
                .put("userId", user.getUserId())
                .put("clientId", user.getClientId())
                .put("username", user.getUsername())
                .put("createdAt", user.getCreatedAt())
                ;
        rc.response()
                .setStatusCode(HttpResponseStatus.CREATED.code())
                .putHeader("content-type", "application/json")
                .end(response.encode());
        return Uni.createFrom().voidItem();
    }

    /**
     * Envia respuesta de error (basado en exception de negocio)
     * @param rc
     * @param failure
     * @return
     */
    private Uni<Void> sendErrorResponse(RoutingContext rc, Throwable failure){
        int statusCode;
        String errorType;
        String message = failure.getMessage() != null ? failure.getMessage() : "Error al registrar usuario";

        // Determinar código de estado según el tipo de excepción
        if (failure instanceof InvalidTokenException) {
            statusCode = HttpResponseStatus.UNAUTHORIZED.code();
            errorType = "Unauthorized";
        } else if (failure instanceof DuplicateCustomerException || failure instanceof DuplicateUserException) {
            statusCode = HttpResponseStatus.CONFLICT.code();
            errorType = "Conflict";
        } else {
            statusCode = HttpResponseStatus.INTERNAL_SERVER_ERROR.code();
            errorType = "Internal Server Error";
        }

        JsonObject error = new JsonObject()
                .put("success", false)
                .put("error", errorType)
                .put("message", message)
                .put("status", statusCode);

        rc.response()
                .setStatusCode(statusCode)
                .putHeader("content-type", "application/json")
                .end(error.encode());
        return Uni.createFrom().voidItem();
    }

    /**
     * Envia respuesta de excepcion inesperada (500)
     * @param rc
     * @param e
     * @return
     */
    private Uni<Void> sendExceptionResponse(RoutingContext rc, Exception e){
        JsonObject error = new JsonObject()
                .put("success", false)
                .put("error", "Internal server error")
                .put("message", "Error inesperado en el registro")
                .put("status", HttpResponseStatus.INTERNAL_SERVER_ERROR.code());

        rc.response()
                .setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                .putHeader("content-type", "application/json")
                .end(error.encode());

        return Uni.createFrom().voidItem();
    }

    /**
     * Envia repuesta de validacion fallida (400
     */
    private void sendBadRequest(RoutingContext rc, String message){
        if(rc.response().ended()){
            log.warn("Response already ended");
            return;
        }
        JsonObject error = new JsonObject()
                .put("success", false)
                .put("error", "Bad request")
                .put("message", message)
                .put("status", HttpResponseStatus.BAD_REQUEST.code());

        rc.response()
                .setStatusCode(HttpResponseStatus.BAD_REQUEST.code())
                .putHeader("content-type", "application/json")
                .end(error.encode());
    }

    /**
     * Maneja errores de validación (400 Bad Request).
     */
    private void handleBadRequest(RoutingContext rc, String message) {
        if (rc.response().ended()) {
            log.warn("Response already sent, skipping bad request handling for: {}", message);
            return;
        }
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
}
