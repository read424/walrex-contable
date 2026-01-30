package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.application.dto.response.LoginResponse;
import org.walrex.domain.model.User;

/**
 * Caso de uso compartido para construir la respuesta completa de login.
 *
 * Encapsula la obtención de datos del cliente (customer, wallets, transacciones recientes)
 * y la generación de tokens JWT.
 *
 * Utilizado por LoginService y BiometricLoginService.
 */
public interface BuildLoginResponseUseCase {

    Uni<LoginResponse> buildResponse(User user);
}
