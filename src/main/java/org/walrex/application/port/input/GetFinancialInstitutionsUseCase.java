package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.application.dto.response.FinancialInstitutionResponse;

import java.util.List;

public interface GetFinancialInstitutionsUseCase {
    Uni<List<FinancialInstitutionResponse>> getByMethodAndCountry(String methodType, String countryIso2);
}
