package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.application.dto.request.RegisterUserRequest;
import org.walrex.domain.model.RegisteredUser;

public interface RegisterUserUseCase {
    Uni<RegisteredUser> register(RegisterUserRequest request);
}
