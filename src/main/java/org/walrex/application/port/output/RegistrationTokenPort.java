package org.walrex.application.port.output;

import org.walrex.domain.model.RegistrationToken;

public interface RegistrationTokenPort {

    RegistrationToken generate(String target, String purpose);

    /**
     * Valida el token JWT y extrae el claim target.
     * @param token El token JWT a validar
     * @return El valor del claim "target" si el token es válido
     * @throws org.walrex.domain.exception.InvalidTokenException si el token es inválido o expirado
     */
    String validateAndExtractTarget(String token);
}
