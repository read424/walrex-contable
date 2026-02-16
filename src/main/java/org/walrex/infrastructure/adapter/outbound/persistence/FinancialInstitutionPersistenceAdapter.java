package org.walrex.infrastructure.adapter.outbound.persistence;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.walrex.application.port.output.FinancialInstitutionQueryPort;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.InstitutionPayoutRailEntity;
import org.walrex.infrastructure.adapter.outbound.persistence.repository.InstitutionPayoutRailRepository;

import java.util.List;

@ApplicationScoped
public class FinancialInstitutionPersistenceAdapter implements FinancialInstitutionQueryPort {

    @Inject
    InstitutionPayoutRailRepository repository;

    @Override
    public Uni<List<InstitutionPayoutRailEntity>> findByRailCodeAndCountryIso2(String methodType, String countryIso2) {
        return repository.findByRailCodeAndCountryIso2(methodType, countryIso2);
    }
}
