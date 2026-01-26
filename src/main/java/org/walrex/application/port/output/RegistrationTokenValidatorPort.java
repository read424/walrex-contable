package org.walrex.application.port.output;

import io.smallrye.mutiny.Uni;

public interface RegistrationTokenValidatorPort {
    /**
     * Valida el token de registro y extrae el target (email o teléfono).
     * @param token Token JWT a validar
     * @return El target extraído del token (email o teléfono)
     * @throws org.walrex.domain.exception.InvalidTokenException si el token es inválido o expirado
     */
    Uni<String> validate(String token);
}
