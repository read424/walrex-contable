package org.walrex.domain.service;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.walrex.application.dto.response.LoginResponse;
import org.walrex.application.port.input.BiometricLoginUseCase;
import org.walrex.application.port.input.BuildLoginResponseUseCase;
import org.walrex.application.port.output.UserRepositoryPort;
import org.walrex.domain.exception.InvalidCustomerDataException;
import org.walrex.domain.exception.InvalidTokenException;
import org.walrex.domain.model.User;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Slf4j
@ApplicationScoped
public class BiometricLoginService implements BiometricLoginUseCase {

    @Inject
    JWTParser jwtParser;

    @Inject
    UserRepositoryPort userRepositoryPort;

    @Inject
    BuildLoginResponseUseCase buildLoginResponseUseCase;

    @ConfigProperty(name = "mp.jwt.secret")
    String secret;

    @Override
    @WithTransaction
    public Uni<LoginResponse> loginWithBiometric(String refreshToken) {
        log.debug("Attempting biometric login");

        // Validar JWT
        JsonWebToken jwt;
        try {
            SecretKey secretKey = new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            jwt = jwtParser.verify(refreshToken, secretKey);
        } catch (ParseException e) {
            log.warn("Invalid or expired refresh token for biometric login: {}", e.getMessage());
            return Uni.createFrom().failure(new InvalidTokenException("Token inválido o expirado"));
        }

        // Extraer userId del subject
        String subject = jwt.getSubject();
        if (subject == null) {
            return Uni.createFrom().failure(new InvalidTokenException("Token sin identificación de usuario"));
        }

        Integer userId;
        try {
            userId = Integer.valueOf(subject);
        } catch (NumberFormatException e) {
            return Uni.createFrom().failure(new InvalidTokenException("Token con identificación inválida"));
        }

        // Buscar usuario y verificar biometría
        return userRepositoryPort.findById(userId)
                .flatMap(userOpt -> {
                    if (userOpt == null || userOpt.isEmpty()) {
                        return Uni.createFrom().failure(new InvalidCustomerDataException("Usuario no encontrado"));
                    }

                    User user = userOpt.get();

                    if (!Boolean.TRUE.equals(user.getBiometricEnabled())) {
                        log.warn("Biometric login attempt for user {} with biometric disabled", userId);
                        return Uni.createFrom().failure(
                                new InvalidCustomerDataException("Autenticación biométrica no habilitada para este usuario"));
                    }

                    log.info("Biometric login successful for userId: {}", userId);
                    return buildLoginResponseUseCase.buildResponse(user);
                });
    }
}
