package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.application.dto.response.ExchangeRateQueryResponse;

public interface GetExchangeRateUseCase {
    Uni<ExchangeRateQueryResponse> getRate(
            String fromCountry, String fromCurrency,
            String toCountry, String toCurrency);
}
