package org.walrex.infrastructure.adapter.outbound.persistence.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.CountryEntity;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.CurrencyEntity;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.ExchangeRateTypeEntity;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.PriceExchangeEntity;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeRateWithDetailsDto {
    private PriceExchangeEntity priceExchange;
    private CurrencyEntity baseCurrency;
    private CurrencyEntity quoteCurrency;
    private ExchangeRateTypeEntity exchangeRateSource;
    private CountryEntity country;
}