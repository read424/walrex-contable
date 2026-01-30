package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.User;

public interface UpdateUserBiometricUseCase {
    Uni<User> updateBiometric(Integer userId, Boolean enabled, String biometricType);
}
