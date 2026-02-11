package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.application.dto.response.CountryRateTypesResponse;

public interface GetCountryRateTypesUseCase {
    Uni<CountryRateTypesResponse> getRateTypesByCountry(String countryIso2);
}