package org.walrex.domain.service;

import io.smallrye.jwt.build.Jwt;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.walrex.application.dto.response.LoginResponse;
import org.walrex.domain.model.User;

import java.util.Arrays;
import java.util.HashSet;

@ApplicationScoped
public class TokenService {

    @ConfigProperty(name = "mp.jwt.verify.issuer")
    String issuer;

    @ConfigProperty(name = "mp.jwt.secret")
    String secret;

    public Uni<LoginResponse> generateTokens(User user) {
        long accessTokenExpiration = System.currentTimeMillis() / 1000 + 600; // 10 minutes
        long refreshTokenExpiration = System.currentTimeMillis() / 1000 + 1296000; // 15 days

        String accessToken = Jwt.issuer(issuer)
                .subject(user.getId().toString())
                .upn(user.getUsername())
                .groups(new HashSet<>(Arrays.asList("user"))) // Asignar roles/grupos si es necesario
                .expiresAt(accessTokenExpiration)
                .signWithSecret(secret);

        String refreshToken = Jwt.issuer(issuer)
                .subject(user.getId().toString())
                .upn("refresh_token")
                .expiresAt(refreshTokenExpiration)
                .signWithSecret(secret);

        return Uni.createFrom().item(LoginResponse.builder()
                .accessToken(accessToken)
                .expiresIn(600L)
                .refreshToken(refreshToken)
                .refreshExpiresIn(1296000L)
                .tokenType("Bearer")
                .build());
    }
}
