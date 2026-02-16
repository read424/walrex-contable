package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.Beneficiary;

public interface CreateBeneficiaryUseCase {
    Uni<Beneficiary> create(Beneficiary beneficiary);
}
