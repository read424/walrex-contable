package org.walrex.infrastructure.adapter.outbound.persistence.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.BeneficiaryAccountEntity;

@ApplicationScoped
public class BeneficiaryAccountPanacheRepository implements PanacheRepositoryBase<BeneficiaryAccountEntity, Integer> {
    // This class can be extended with custom repository methods if needed in the future.
}
