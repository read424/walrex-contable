package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.application.dto.query.BeneficiaryFilter;
import org.walrex.application.dto.query.PageRequest;
import org.walrex.application.dto.response.BeneficiarySearchResponse;
import org.walrex.application.dto.response.PagedResponse;

public interface ListBeneficiaryUseCase {
    Uni<PagedResponse<BeneficiarySearchResponse>> list(PageRequest pageRequest, BeneficiaryFilter filter);
}
