package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.application.dto.request.RefreshTokenRequest;
import org.walrex.application.dto.response.LoginResponse;

public interface RefreshTokenUseCase {
    Uni<LoginResponse> refreshToken(RefreshTokenRequest refreshTokenRequest);
}
