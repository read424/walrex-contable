package org.walrex.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RemittanceExchangeRate {
    private Long id;
    private String typeOperation;
    private Long baseCurrencyId;
    private Long quoteCurrencyId;
    private BigDecimal rate;
    private String isActive;
    private LocalDate exchangeDate;
    private Long exchangeRateSourceId;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;

    // Relaciones
    private Currency baseCurrency;
    private Currency quoteCurrency;
    private ExchangeRateSource exchangeRateSource;

    public static RemittanceExchangeRate create(String typeOperation, Long baseCurrencyId, Long quoteCurrencyId, 
                                              BigDecimal rate, Long exchangeRateSourceId, LocalDate exchangeDate) {
        return RemittanceExchangeRate.builder()
                .typeOperation(typeOperation)
                .baseCurrencyId(baseCurrencyId)
                .quoteCurrencyId(quoteCurrencyId)
                .rate(rate)
                .exchangeRateSourceId(exchangeRateSourceId)
                .exchangeDate(exchangeDate)
                .isActive("1")
                .createdAt(ZonedDateTime.now())
                .updatedAt(ZonedDateTime.now())
                .build();
    }
}