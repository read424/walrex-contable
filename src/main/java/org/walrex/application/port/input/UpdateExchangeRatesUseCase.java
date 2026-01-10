package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.ExchangeRateUpdate;

public interface UpdateExchangeRatesUseCase {
    Uni<ExchangeRateUpdate> updateExchangeRates();
}
