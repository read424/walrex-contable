package org.walrex.infrastructure.adapter.outbound.persistence.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.ClientScreeningHistoryEntity;

import java.util.List;

@ApplicationScoped
public class ClientScreeningHistoryRepository
        implements PanacheRepositoryBase<ClientScreeningHistoryEntity, Integer> {

    public Uni<List<ClientScreeningHistoryEntity>> findByClientId(Integer clientId) {
        return list("clientId = ?1 order by checkedAt desc", clientId);
    }
}
