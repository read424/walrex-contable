package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.ExchangeCalculation;

import java.math.BigDecimal;

public interface CalculateExchangeRateUseCase {
    Uni<ExchangeCalculation> calculateExchangeRate(
            BigDecimal amount,
            String baseCurrency,
            String quoteCurrency,
            BigDecimal margin
    );
}