package org.walrex.domain.service;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.walrex.application.port.input.UpdateUserBiometricUseCase;
import org.walrex.application.port.output.UserRepositoryPort;
import org.walrex.domain.model.User;

import java.time.OffsetDateTime;
import java.util.Optional;

@Slf4j
@ApplicationScoped
public class UserService implements UpdateUserBiometricUseCase {

    @Inject
    UserRepositoryPort userRepository;

    @Override
    @WithTransaction
    public Uni<User> updateBiometric(Integer userId, Boolean enabled, String biometricType) {
        log.debug("Updating biometric for userId={}, enabled={}, type={}", userId, enabled, biometricType);

        return userRepository.findById(userId)
                .onItem().transformToUni(opt -> {
                    Optional<User> maybe = opt;
                    if (maybe.isEmpty()) {
                        return Uni.createFrom().failure(new IllegalArgumentException("User not found"));
                    }
                    User user = maybe.get();
                    boolean e = Boolean.TRUE.equals(enabled);
                    user.setBiometricEnabled(e);
                    user.setBiometricType(e ? biometricType : null);
                    user.setBiometricEnrolledAt(e ? OffsetDateTime.now() : null);

                    return userRepository.update(user);
                });
    }
}
