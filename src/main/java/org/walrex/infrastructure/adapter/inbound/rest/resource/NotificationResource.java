package org.walrex.infrastructure.adapter.inbound.rest.resource;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.walrex.application.dto.RegisterDeviceTokenRequest;
import org.walrex.application.port.input.RegisterDeviceTokenUseCase;
import org.walrex.infrastructure.adapter.inbound.rest.security.JwtSecurityInterceptor;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Path("/api/v1/notifications/fcm")
@ApplicationScoped
public class NotificationResource {

    @Inject
    JwtSecurityInterceptor jwtSecurityInterceptor;

    @Inject
    RegisterDeviceTokenUseCase registerDeviceTokenUseCase;

    @POST
    @Path("/register")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @WithTransaction
    public Uni<Response> registerDevice(
            @HeaderParam("Authorization") String authHeader,
            @Valid RegisterDeviceTokenRequest request) {

        Optional<Integer> userIdOpt = jwtSecurityInterceptor.authenticateAndGetUserId(authHeader);
        if (userIdOpt.isEmpty()) {
            return Uni.createFrom().item(Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("error", "Unauthorized", "message", "Missing or invalid token"))
                    .build());
        }

        Integer userId = userIdOpt.get();
        log.info("=== [FCM REGISTER] userId: {} ===", userId);

        return registerDeviceTokenUseCase.register(userId, request.getToken(), request.getPlatform())
                .map(deviceToken -> Response.status(Response.Status.CREATED)
                        .entity(Map.of(
                                "success", true,
                                "message", "Device registered successfully"
                        ))
                        .build());
    }
}
