package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.application.dto.request.LoginRequest;
import org.walrex.application.dto.response.LoginResponse;

public interface LoginUseCase {
    Uni<LoginResponse> login(LoginRequest loginRequest);
}
