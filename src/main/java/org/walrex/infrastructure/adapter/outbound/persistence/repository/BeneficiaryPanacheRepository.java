package org.walrex.infrastructure.adapter.outbound.persistence.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.BeneficiaryEntity;

@ApplicationScoped
public class BeneficiaryPanacheRepository implements PanacheRepositoryBase<BeneficiaryEntity, Long> {
}
