package org.walrex.infrastructure.adapter.outbound.persistence;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.walrex.application.port.output.ScreeningHistoryRepositoryPort;
import org.walrex.domain.model.ClientScreeningHistory;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.ClientScreeningHistoryEntity;
import org.walrex.infrastructure.adapter.outbound.persistence.repository.ClientScreeningHistoryRepository;

import java.time.OffsetDateTime;

@ApplicationScoped
public class ScreeningHistoryPersistenceAdapter implements ScreeningHistoryRepositoryPort {

    @Inject
    ClientScreeningHistoryRepository repository;

    @Override
    public Uni<Void> save(ClientScreeningHistory history) {
        ClientScreeningHistoryEntity entity = ClientScreeningHistoryEntity.builder()
                .clientId(history.getClientId())
                .decision(history.getDecision())
                .score(history.getScore())
                .datasets(history.getDatasets())
                .entityId(history.getEntityId())
                .identifierMatched(history.getIdentifierMatched())
                .triggeredBy(history.getTriggeredBy())
                .checkedAt(history.getCheckedAt() != null ? history.getCheckedAt() : OffsetDateTime.now())
                .build();

        return repository.persist(entity).replaceWithVoid();
    }
}
