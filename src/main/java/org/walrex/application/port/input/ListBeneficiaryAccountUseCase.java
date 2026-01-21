package org.walrex.application.port.input;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.walrex.application.dto.query.BeneficiaryAccountFilter;
import org.walrex.application.dto.query.PageRequest;
import org.walrex.application.dto.response.BeneficiaryAccountResponse;
import org.walrex.application.dto.response.PagedResponse;

import java.util.List;

public interface ListBeneficiaryAccountUseCase {
    Uni<PagedResponse<BeneficiaryAccountResponse>> list(PageRequest pageRequest, BeneficiaryAccountFilter filter);
    Multi<BeneficiaryAccountResponse> streamAll();
    Multi<BeneficiaryAccountResponse> streamWithFilter(BeneficiaryAccountFilter filter);
    Uni<List<BeneficiaryAccountResponse>> listByCustomerId(Long customerId);
}
