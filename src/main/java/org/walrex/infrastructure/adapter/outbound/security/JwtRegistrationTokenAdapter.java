package org.walrex.infrastructure.adapter.outbound.security;

import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.keys.HmacKey;
import org.walrex.application.port.output.RegistrationTokenPort;
import org.walrex.domain.model.RegistrationToken;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

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
    public boolean validate(String token, String expectedTarget) {
        try {
            JwtConsumer jwtConsumer = new JwtConsumerBuilder()
                    .setRequireExpirationTime()
                    .setExpectedIssuer(ISSUER)
                    .setExpectedSubject(expectedTarget)
                    .setVerificationKey(new HmacKey(secret.getBytes(StandardCharsets.UTF_8)))
                    .build();

            JwtClaims claims = jwtConsumer.processToClaims(token);
            String target = claims.getStringClaimValue(CLAIM_TARGET);
            return expectedTarget.equals(target);
        } catch (Exception e) {
            return false;
        }
    }
}
