package org.walrex.infrastructure.adapter.outbound.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.walrex.application.dto.response.*;
import org.walrex.domain.model.Country;
import org.walrex.domain.model.Currency;
import org.walrex.domain.model.ExchangeRateSource;
import org.walrex.domain.model.RemittanceExchangeRate;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.CountryEntity;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.CurrencyEntity;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.ExchangeRateTypeEntity;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.PriceExchangeEntity;

import java.math.BigDecimal;

@Mapper(componentModel = MappingConstants.ComponentModel.CDI)
public interface ExchangeRateMapper {

    // Entity to Domain mappings
    @Mapping(target = "sourceCode", source = "codeRate")
    @Mapping(target = "sourceName", source = "nameRate")
    @Mapping(target = "currencyId", source = "baseCurrencyId")
    @Mapping(target = "description", source = "nameRate")
    @Mapping(target = "createdAt", expression = "java(entity.getCreatedAt() != null ? entity.getCreatedAt().atZoneSameInstant(java.time.ZoneOffset.UTC) : null)")
    @Mapping(target = "updatedAt", expression = "java(entity.getUpdatedAt() != null ? entity.getUpdatedAt().atZoneSameInstant(java.time.ZoneOffset.UTC) : null)")
    @Mapping(target = "country", ignore = true)
    @Mapping(target = "currency", ignore = true)
    ExchangeRateSource toDomain(ExchangeRateTypeEntity entity);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "typeOperation", source = "typeOperation")
    @Mapping(target = "baseCurrencyId", source = "idCurrencyBase")
    @Mapping(target = "quoteCurrencyId", source = "idCurrencyQuote")
    @Mapping(target = "rate", source = "amountPrice")
    @Mapping(target = "exchangeDate", source = "dateExchange")
    @Mapping(target = "exchangeRateSourceId", ignore = true)
    @Mapping(target = "createdAt", expression = "java(entity.getCreatedAt() != null ? entity.getCreatedAt().atZoneSameInstant(java.time.ZoneOffset.UTC) : null)")
    @Mapping(target = "updatedAt", expression = "java(entity.getUpdatedAt() != null ? entity.getUpdatedAt().atDate(java.time.LocalDate.now()).atZoneSameInstant(java.time.ZoneOffset.UTC) : null)")
    @Mapping(target = "baseCurrency", ignore = true)
    @Mapping(target = "quoteCurrency", ignore = true)
    @Mapping(target = "exchangeRateSource", ignore = true)
    RemittanceExchangeRate toDomain(PriceExchangeEntity entity);

    // Domain to Response mappings
    @Mapping(target = "iso2", source = "alphabeticCode2")
    @Mapping(target = "iso3", source = "alphabeticCode3")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "flagEmoji", source = "unicodeFlag")
    CountryInfoResponse toCountryInfoResponse(Country country);

    @Mapping(target = "code", source = "alphabeticCode")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "symbol", source = "symbol")
    CurrencyInfoResponse toCurrencyInfoResponse(Currency currency);

    // Entity to Response direct mappings
    @Mapping(target = "iso2", source = "alphabeticCode2")
    @Mapping(target = "iso3", source = "alphabeticCode3")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "flagEmoji", source = "unicodeFlag")
    CountryInfoResponse toCountryInfoResponse(CountryEntity entity);

    @Mapping(target = "code", source = "alphabeticCode")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "symbol", source = "symbol")
    CurrencyInfoResponse toCurrencyInfoResponse(CurrencyEntity entity);

    // Manual mapping methods for complex cases
    default ExchangeRateInfoResponse toExchangeRateInfoResponse(ExchangeRateSource source, BigDecimal rate) {
        if (source == null) {
            return null;
        }
        return ExchangeRateInfoResponse.builder()
                .type(source.getSourceCode())
                .rate(rate)
                .label(source.getSourceName())
                .build();
    }

    default ExchangeRateInfoResponse toExchangeRateInfoResponse(ExchangeRateTypeEntity entity, BigDecimal rate) {
        if (entity == null) {
            return null;
        }
        return ExchangeRateInfoResponse.builder()
                .type(entity.getCodeRate())
                .rate(rate)
                .label(entity.getNameRate())
                .build();
    }
}