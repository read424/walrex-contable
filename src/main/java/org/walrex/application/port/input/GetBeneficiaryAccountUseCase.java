package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.BeneficiaryAccount;

public interface GetBeneficiaryAccountUseCase {
    Uni<BeneficiaryAccount> findById(Long id);
}
