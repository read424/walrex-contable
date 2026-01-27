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

import java.util.Optional;

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

    @Override
    public Uni<Optional<User>> findByUsername(String username) {
        return userRepository.find("username", username).firstResult()
                .map(userEntity -> Optional.ofNullable(userEntityMapper.toDomain(userEntity)));
    }

    @Override
    public Uni<Optional<User>> findById(Integer id) {
        return userRepository.findById(id)
                .map(userEntity -> Optional.ofNullable(userEntityMapper.toDomain(userEntity)));
    }

    @Override
    public Uni<User> update(User user) {
        return userRepository.findById(user.getId())
                .onItem().transformToUni(existingEntity -> {
                    if (existingEntity == null) {
                        return Uni.createFrom().failure(
                                new IllegalArgumentException("User not found with id: " + user.getId())
                        );
                    }

                    // Actualizar solo los campos modificables
                    existingEntity.setPinAttempts(user.getPinAttempts());
                    existingEntity.setPinLockedUntil(user.getPinLockedUntil());
                    existingEntity.setMfaEnabled(user.isMfaEnabled());
                    existingEntity.setMfaType(user.getMfaType());
                    existingEntity.setActive(user.getActive());

                    // persist() actualizará la entidad existente porque ya está managed
                    return userRepository.persist(existingEntity)
                            .onItem().transform(userEntityMapper::toDomain);
                });
    }
}
