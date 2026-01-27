package org.walrex.infrastructure.adapter.outbound.persistence;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.walrex.application.port.output.RefreshTokenRepositoryPort;
import org.walrex.domain.model.RefreshToken;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.RefreshTokenEntity;
import org.walrex.infrastructure.adapter.outbound.persistence.mapper.RefreshTokenEntityMapper;
import org.walrex.infrastructure.adapter.outbound.persistence.repository.RefreshTokenRepository;

import java.util.Optional;

@ApplicationScoped
public class RefreshTokenRepositoryAdapter implements RefreshTokenRepositoryPort {

    @Inject
    RefreshTokenRepository refreshTokenRepository;

    @Inject
    RefreshTokenEntityMapper refreshTokenEntityMapper;

    @Override
    public Uni<Optional<RefreshToken>> findByTokenHash(String tokenHash) {
        return refreshTokenRepository.find("refreshTokenHash", tokenHash).firstResult()
                .map(entity -> Optional.ofNullable(refreshTokenEntityMapper.toDomain(entity)));
    }

    @Override
    public Uni<RefreshToken> save(RefreshToken refreshToken) {
        RefreshTokenEntity entity = refreshTokenEntityMapper.toEntity(refreshToken);
        return refreshTokenRepository.persist(entity)
                .map(savedEntity -> refreshTokenEntityMapper.toDomain(savedEntity));
    }

    @Override
    public Uni<RefreshToken> update(RefreshToken refreshToken) {
        RefreshTokenEntity entity = refreshTokenEntityMapper.toEntity(refreshToken);
        return refreshTokenRepository.getSession()
                .chain(session -> session.merge(entity))
                .map(updatedEntity -> refreshTokenEntityMapper.toDomain(updatedEntity));
    }
}
