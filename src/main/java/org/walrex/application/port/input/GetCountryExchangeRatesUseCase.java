package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.application.dto.response.CountryRatesResponse;

public interface GetCountryExchangeRatesUseCase {
    Uni<CountryRatesResponse> getExchangeRatesByCountry(String countryIso2);
}