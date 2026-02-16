package org.walrex.application.port.output;

import io.smallrye.mutiny.Uni;
import org.walrex.application.dto.query.BeneficiaryFilter;
import org.walrex.application.dto.query.PageRequest;
import org.walrex.domain.model.Beneficiary;
import org.walrex.domain.model.PagedResult;

import org.walrex.application.dto.response.BeneficiarySearchResponse;

import java.util.Optional;

public interface BeneficiaryQueryPort {
    Uni<PagedResult<BeneficiarySearchResponse>> findAll(PageRequest pageRequest, BeneficiaryFilter filter);
    Uni<Optional<Beneficiary>> findOneById(Long id);
}
