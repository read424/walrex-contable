package org.walrex.infrastructure.adapter.outbound.security;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.walrex.application.port.output.RegistrationTokenPort;
import org.walrex.application.port.output.RegistrationTokenValidatorPort;

@Slf4j
@ApplicationScoped
public class RegistrationTokenValidatorAdapter implements RegistrationTokenValidatorPort {

    @Inject
    RegistrationTokenPort registrationTokenPort;

    @Override
    public Uni<String> validate(String token) {
        return Uni.createFrom().item(() -> {
            log.debug("Validating registration token");

            // Validar el token y extraer el target (lanza excepción si es inválido)
            String target = registrationTokenPort.validateAndExtractTarget(token);

            log.debug("Token validated successfully, target: {}", target);
            return target;
        });
    }
}
