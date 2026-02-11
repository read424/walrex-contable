package org.walrex.infrastructure.adapter.outbound.persistence;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.walrex.application.port.output.DeviceTokenRepositoryPort;
import org.walrex.domain.model.DeviceToken;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.DeviceTokenEntity;
import org.walrex.infrastructure.adapter.outbound.persistence.mapper.DeviceTokenEntityMapper;
import org.walrex.infrastructure.adapter.outbound.persistence.repository.DeviceTokenRepository;

import java.util.List;

@ApplicationScoped
public class DeviceTokenRepositoryAdapter implements DeviceTokenRepositoryPort {

    @Inject
    DeviceTokenRepository repository;

    @Inject
    DeviceTokenEntityMapper mapper;

    @Override
    public Uni<DeviceToken> save(DeviceToken deviceToken) {
        DeviceTokenEntity entity = mapper.toEntity(deviceToken);
        return Panache.withTransaction(() ->
                repository.getSession()
                        .chain(session -> session.merge(entity))
        ).map(mapper::toDomain);
    }

    @Override
    public Uni<List<DeviceToken>> findActiveByUserId(Integer userId) {
        return Panache.withSession(() ->
                repository.findActiveByUserId(userId)
        ).map(mapper::toDomainList);
    }

    @Override
    public Uni<DeviceToken> findByToken(String token) {
        return Panache.withSession(() ->
                repository.findByToken(token)
        ).map(entity -> entity != null ? mapper.toDomain(entity) : null);
    }

    @Override
    public Uni<List<DeviceToken>> findAllActive() {
        return Panache.withSession(() ->
                repository.findAllActive()
        ).map(mapper::toDomainList);
    }

    @Override
    public Uni<Void> deactivate(String token) {
        return Panache.withTransaction(() ->
                repository.findByToken(token)
                        .onItem().ifNotNull().invoke(entity -> entity.setActive(false))
                        .replaceWithVoid()
        );
    }
}
