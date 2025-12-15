package org.walrex.infrastructure.adapter.outbound.persistence.mapper;

import org.mapstruct.*;
import org.walrex.domain.model.CountryCurrency;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.CountryCurrencyEntity;

/**
 * Mapper entre CountryCurrencyEntity (persistencia) y CountryCurrency (dominio).
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.CDI,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR
)
public interface CountryCurrencyMapper {

    @Mapping(source = "country.id", target = "countryId")
    @Mapping(source = "currency.id", target = "currencyId")
    @Mapping(source = "currency.alphabeticCode", target = "currencyCode")
    @Mapping(source = "currency.name", target = "currencyName")
    CountryCurrency toDomain(CountryCurrencyEntity entity);

    @Mapping(source = "countryId", target = "country.id")
    @Mapping(source = "currencyId", target = "currency.id")
    CountryCurrencyEntity toEntity(CountryCurrency domain);
}