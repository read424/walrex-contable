package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.BeneficiaryAccount;

public interface CreateBeneficiaryAccountUseCase {
    Uni<BeneficiaryAccount> create(BeneficiaryAccount beneficiaryAccount);
}
