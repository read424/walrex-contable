package org.walrex.domain.service;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.walrex.application.dto.request.LoginRequest;
import org.walrex.application.dto.response.LoginResponse;
import org.walrex.application.port.input.LoginUseCase;
import org.walrex.application.port.output.UserRepositoryPort;
import org.walrex.domain.exception.InvalidCustomerDataException;
import org.walrex.domain.model.User;

import java.time.OffsetDateTime;

@ApplicationScoped
public class LoginService implements LoginUseCase {

    @Inject
    UserRepositoryPort userRepositoryPort;

    @Inject
    TokenService tokenService;

    @ConfigProperty(name = "app.login.max-attempts", defaultValue = "5")
    int maxLoginAttempts;

    @ConfigProperty(name = "app.login.lockout-minutes", defaultValue = "15")
    int lockoutMinutes;

    @Override
    @WithTransaction
    public Uni<LoginResponse> login(LoginRequest loginRequest) {
        return userRepositoryPort.findByUsername(loginRequest.username())
                .flatMap(userOpt -> {
                    if (userOpt == null || userOpt.isEmpty()) {
                        return Uni.createFrom().failure(new InvalidCustomerDataException("Usuario o pin incorrectos"));
                    }

                    User user = userOpt.get();

                    if (user.getPinLockedUntil() != null && user.getPinLockedUntil().isAfter(OffsetDateTime.now())) {
                        return Uni.createFrom().failure(new InvalidCustomerDataException("La cuenta está bloqueada. Intente más tarde."));
                    }

                    if (loginRequest.pinHash().equals(user.getPinHash())) {
                        user.setPinAttempts(0);
                        user.setPinLockedUntil(null);
                        return userRepositoryPort.update(user)
                                .flatMap(tokenService::generateTokens);
                    } else {
                        user.setPinAttempts(user.getPinAttempts() + 1);
                        if (user.getPinAttempts() >= maxLoginAttempts) {
                            user.setPinLockedUntil(OffsetDateTime.now().plusMinutes(lockoutMinutes));
                        }
                        return userRepositoryPort.update(user)
                                .flatMap(u -> Uni.createFrom().failure(new InvalidCustomerDataException("Usuario o pin incorrectos")));
                    }
                });
    }
}