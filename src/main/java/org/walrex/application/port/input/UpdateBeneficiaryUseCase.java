package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.Beneficiary;

public interface UpdateBeneficiaryUseCase {
    Uni<Beneficiary> update(Long id, Beneficiary beneficiary);
}
