package org.walrex.infrastructure.adapter.inbound.rest;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.walrex.application.dto.request.LoginRequest;
import org.walrex.application.dto.request.RefreshTokenRequest;
import org.walrex.application.dto.response.LoginResponse;
import org.walrex.application.port.input.LoginUseCase;
import org.walrex.application.port.input.RefreshTokenUseCase;

@Path("/api/v1/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    @Inject
    LoginUseCase loginUseCase;

    @Inject
    RefreshTokenUseCase refreshTokenUseCase;

    @POST
    @Path("/login")
    public Uni<Response> login(LoginRequest loginRequest) {
        return loginUseCase.login(loginRequest)
                .map(loginResponse -> Response.ok(loginResponse).build());
    }

    @POST
    @Path("/refresh")
    public Uni<Response> refresh(RefreshTokenRequest refreshTokenRequest) {
        return refreshTokenUseCase.refreshToken(refreshTokenRequest)
                .map(loginResponse -> Response.ok(loginResponse).build());
    }
}
