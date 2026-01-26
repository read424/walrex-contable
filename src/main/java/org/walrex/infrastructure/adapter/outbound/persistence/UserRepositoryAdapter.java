package org.walrex.infrastructure.adapter.outbound.persistence;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.walrex.application.port.output.UserRepositoryPort;
import org.walrex.domain.exception.DuplicateUserException;
import org.walrex.domain.model.User;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.UserEntity;
import org.walrex.infrastructure.adapter.outbound.persistence.mapper.UserEntityMapper;
import org.walrex.infrastructure.adapter.outbound.persistence.repository.UserRepository;

@Slf4j
@ApplicationScoped
public class UserRepositoryAdapter implements UserRepositoryPort {

    @Inject
    UserRepository userRepository;

    @Inject
    UserEntityMapper userEntityMapper;

    @Override
    public Uni<Long> save(User user) {
        log.debug("Saving user: username={}, type={}", user.getUsername(), user.getUsernameType());

        // Verificar si el username ya existe
        return userRepository.existsByUsername(user.getUsername())
                .onItem().transformToUni(exists -> {
                    if (exists) {
                        log.warn("Duplicate username found: {}", user.getUsername());
                        return Uni.createFrom().failure(
                                new DuplicateUserException("El usuario ya existe: " + user.getUsername())
                        );
                    }

                    // Mapear a entidad y persistir
                    UserEntity entity = userEntityMapper.toEntity(user);
                    log.debug("Persisting user entity");

                    return userRepository.persist(entity)
                            .onItem().transform(savedEntity -> {
                                log.info("User saved with ID: {}", savedEntity.getId());
                                return savedEntity.getId().longValue();
                            });
                });
    }
}
