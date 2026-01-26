package org.walrex.infrastructure.adapter.outbound.security;

import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.keys.HmacKey;
import org.walrex.application.port.output.RegistrationTokenPort;
import org.walrex.domain.exception.InvalidTokenException;
import org.walrex.domain.model.RegistrationToken;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

@Slf4j
@ApplicationScoped
public class JwtRegistrationTokenAdapter implements RegistrationTokenPort {

    private static final String ISSUER = "walrex-otp-service";
    private static final String CLAIM_TARGET = "target";
    private static final String CLAIM_PURPOSE = "purpose";

    @ConfigProperty(name = "app.registration.token.expiration-minutes", defaultValue = "10")
    int expirationMinutes;

    @ConfigProperty(name = "app.registration.token.secret")
    String secret;

    @Override
    public RegistrationToken generate(String target, String purpose) {
        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(expirationMinutes));

        String token = Jwt.issuer(ISSUER)
                .subject(target)
                .claim(CLAIM_TARGET, target)
                .claim(CLAIM_PURPOSE, purpose)
                .expiresAt(expiresAt)
                .signWithSecret(secret);

        return new RegistrationToken(token, expiresAt);
    }

    @Override
    public String validateAndExtractTarget(String token) {
        log.debug("Validating JWT registration token");
        try {
            JwtConsumer jwtConsumer = new JwtConsumerBuilder()
                    .setRequireExpirationTime()
                    .setExpectedIssuer(ISSUER)
                    .setVerificationKey(new HmacKey(secret.getBytes(StandardCharsets.UTF_8)))
                    .build();

            JwtClaims claims = jwtConsumer.processToClaims(token);
            String target = claims.getStringClaimValue(CLAIM_TARGET);

            if (target == null || target.isBlank()) {
                log.warn("Token is missing 'target' claim");
                throw new InvalidTokenException("El token no contiene el claim 'target'");
            }

            log.debug("Token validated successfully, target: {}", target);
            return target;
        } catch (InvalidJwtException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            throw new InvalidTokenException("El token de registro no es válido: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error validating token", e);
            throw new InvalidTokenException("Error al validar el token de registro");
        }
    }
}
