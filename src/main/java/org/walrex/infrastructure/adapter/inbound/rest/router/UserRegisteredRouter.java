package org.walrex.infrastructure.adapter.inbound.rest.router;

import io.quarkus.vertx.web.Route;
import io.quarkus.vertx.web.RouteBase;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.walrex.application.dto.request.RegisterUserRequest;

@ApplicationScoped
@RouteBase(path = "/api/v1/users", produces = MediaType.APPLICATION_JSON)
@Tag(name = "User registration", description = "API para gestionar registro de usuarios desde el app")
public class UserRegisteredRouter {

    @Inject
    UserRegistrationHandler userRegistrationHandler;

    @Inject
    UserBiometricHandler userBiometricHandler;

    @Route(path = "/register", methods = Route.HttpMethod.POST)
    @Operation(
            summary = "Crear cuenta de usuario",
            description = "Crear un usuario desde el app mobile"
    )
    @RequestBody(
            description = "Datos para crear una cuenta",
            required = true,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = RegisterUserRequest.class),
                    examples = @ExampleObject(
                            name = "",
                            value = """
                                    """
                    )
            )
    )
    @APIResponses(
            @APIResponse(
                    responseCode = "201",
                    description = "",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON
                            //, schema = @Schema(implementation = RegisteredUser.)
                    )
            )
    )
    public Uni<Void> create(RoutingContext rc) {
        return userRegistrationHandler.createdUser(rc);
    }

    @Route(path = "/biometric", methods = Route.HttpMethod.PUT)
    public Uni<Void> biometric(RoutingContext rc) {
        return userBiometricHandler.updateBiometric(rc);
    }
}
