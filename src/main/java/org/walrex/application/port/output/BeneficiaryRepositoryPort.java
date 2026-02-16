package org.walrex.application.port.output;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.Beneficiary;

import java.util.Optional;

public interface BeneficiaryRepositoryPort {
    Uni<Beneficiary> save(Beneficiary beneficiary);
    Uni<Beneficiary> findById(Long id);
    Multi<Beneficiary> findAllByClientId(Integer clientId);
    Uni<Void> deleteById(Long id);
    Uni<Beneficiary> update(Beneficiary beneficiary);
}
