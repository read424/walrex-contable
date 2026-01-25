package org.walrex.application.port.output;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.OutboxEvent;

import java.util.List;

public interface OutboxRepositoryPort {

    Uni<Void> save(OutboxEvent event);

    Uni<List<OutboxEvent>> lockPending(int limit);

    Uni<Void> markAsSent(Long eventId);

    Uni<Void> markAsFailed(Long eventId, String reason);
}
