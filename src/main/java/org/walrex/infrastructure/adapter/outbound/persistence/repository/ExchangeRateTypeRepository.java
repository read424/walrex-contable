package org.walrex.infrastructure.adapter.outbound.persistence.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.ExchangeRateTypeEntity;

import java.util.List;

@ApplicationScoped
public class ExchangeRateTypeRepository implements PanacheRepository<ExchangeRateTypeEntity> {

    public Uni<List<ExchangeRateTypeEntity>> findAllActive() {
        return find("isActive = true ORDER BY displayOrder ASC").list();
    }

    public Uni<ExchangeRateTypeEntity> findByCode(String code) {
        return find("code = ?1 AND isActive = true", code).firstResult();
    }
}