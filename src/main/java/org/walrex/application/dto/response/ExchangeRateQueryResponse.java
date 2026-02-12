package org.walrex.application.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExchangeRateQueryResponse(
        String fromCountry,
        String fromCurrency,
        String toCountry,
        String toCurrency,
        BigDecimal rate,
        LocalDate date
) {
}
