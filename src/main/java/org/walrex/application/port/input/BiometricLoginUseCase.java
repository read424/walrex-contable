package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.application.dto.response.LoginResponse;

/**
 * Caso de uso para login biométrico.
 *
 * Valida el refresh token JWT, verifica que el usuario tenga
 * biometría habilitada, y retorna la respuesta completa de login.
 */
public interface BiometricLoginUseCase {

    Uni<LoginResponse> loginWithBiometric(String refreshToken);
}
