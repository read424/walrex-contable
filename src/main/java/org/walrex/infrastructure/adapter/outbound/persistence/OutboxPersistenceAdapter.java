package org.walrex.infrastructure.adapter.outbound.persistence;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.walrex.application.port.output.OutboxRepositoryPort;
import org.walrex.domain.model.OutboxEvent;
import org.walrex.infrastructure.adapter.outbound.persistence.mapper.OutboxEventMapper;
import org.walrex.infrastructure.adapter.outbound.persistence.repository.OutboxPanacheRepository;

import java.util.List;

@ApplicationScoped
public class OutboxPersistenceAdapter implements OutboxRepositoryPort {

    @Inject
    OutboxEventMapper mapper;

    @Inject
    OutboxPanacheRepository repository;

    @Override
    public Uni<Void> save(OutboxEvent event) {
        var entity = mapper.toEntity(event);
        return repository.save(entity);
    }

    @Override
    public Uni<List<OutboxEvent>> lockPending(int limit) {
        return repository.lockPending(limit)
                .map(entities -> entities.stream()
                        .map(mapper::toDomain)
                        .toList());
    }

    @Override
    public Uni<Void> markAsSent(Long eventId) {
        return repository.markAsSent(eventId);
    }

    @Override
    public Uni<Void> markAsFailed(Long eventId, String reason) {
        return repository.markAsFailed(eventId, reason);
    }
}
