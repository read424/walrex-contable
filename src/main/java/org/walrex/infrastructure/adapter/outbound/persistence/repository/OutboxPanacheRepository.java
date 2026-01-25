package org.walrex.infrastructure.adapter.outbound.persistence.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import org.walrex.domain.model.OutboxStatus;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.OutboxEventEntity;

import java.time.Instant;
import java.util.List;

@ApplicationScoped
public class OutboxPanacheRepository implements PanacheRepository<OutboxEventEntity> {

    public Uni<Void> save(OutboxEventEntity event) {
        return persist(event).replaceWithVoid();
    }

    @Transactional
    public Uni<List<OutboxEventEntity>> lockPending(int limit){
        return find("""
                status = ?1 AND retryCount < maxRetries
                order by createdAt""", OutboxStatus.PENDING)
                .page(0, limit)
                .withLock(LockModeType.PESSIMISTIC_WRITE)
                .list()
                .flatMap(events -> {
                    if(events.isEmpty()) {
                        return Uni.createFrom().item(List.of());
                    }
                    events.forEach(e -> e.setStatus(OutboxStatus.PROCESSING));

                    return persist(events).replaceWith(events);
                });
    }

    public Uni<List<OutboxEventEntity>> findPending() {
        return find("status = ?1 order by createdAt", OutboxStatus.PENDING)
                .list();
    }

    public Uni<Void> markAsSent(Long id) {
        return update("status = 'SENT', processedAt = ?1 where id = ?2 and status = ?3",
                    Instant.now(),
                    id,
                    OutboxStatus.PROCESSING
                )
                .replaceWithVoid();
    }

    public Uni<Void> markAsFailed(Long id, String reason) {
        return findById(id)
                .flatMap(event -> {
                    if (event == null) {
                        return Uni.createFrom().voidItem();
                    }
                    event.setRetryCount(event.getRetryCount() + 1);
                    event.setLastError(reason);
                    event.setProcessedAt(Instant.now());

                    if (event.getRetryCount() >= event.getMaxRetries()) {
                        event.setStatus(OutboxStatus.FAILED);
                    } else {
                        event.setStatus(OutboxStatus.PENDING);
                    }
                    return persist(event).replaceWithVoid();
                });
    }
}
