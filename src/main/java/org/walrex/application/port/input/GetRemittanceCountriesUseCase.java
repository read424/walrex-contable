package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.application.dto.response.CountriesListResponse;

public interface GetRemittanceCountriesUseCase {
    Uni<CountriesListResponse> getAvailableCountries();
}