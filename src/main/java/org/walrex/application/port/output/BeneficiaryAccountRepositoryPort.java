package org.walrex.application.port.output;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.BeneficiaryAccount;

public interface BeneficiaryAccountRepositoryPort {
    Uni<BeneficiaryAccount> save(BeneficiaryAccount beneficiaryAccount);
    Uni<BeneficiaryAccount> update(BeneficiaryAccount beneficiaryAccount);
    Uni<Boolean> softDelete(Integer id);
}
