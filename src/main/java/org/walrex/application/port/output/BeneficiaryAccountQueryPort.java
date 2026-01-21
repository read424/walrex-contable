package org.walrex.application.port.output;

import io.smallrye.mutiny.Uni;
import org.walrex.application.dto.query.BeneficiaryAccountFilter;
import org.walrex.application.dto.query.PageRequest;
import org.walrex.domain.model.BeneficiaryAccount;
import org.walrex.domain.model.PagedResult;

import java.util.Optional;

public interface BeneficiaryAccountQueryPort {
    Uni<PagedResult<BeneficiaryAccount>> findAll(PageRequest pageRequest, BeneficiaryAccountFilter filter);
    Uni<Optional<BeneficiaryAccount>> findById(Integer id);
    Uni<Boolean> existsByAccountNumber(String accountNumber, Integer excludeId);
}
