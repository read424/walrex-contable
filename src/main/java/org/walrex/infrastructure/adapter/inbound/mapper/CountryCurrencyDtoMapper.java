package org.walrex.infrastructure.adapter.inbound.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.walrex.application.dto.response.CountryCurrencyResponse;
import org.walrex.domain.model.CountryCurrency;

/**
 * Mapper entre CountryCurrency (dominio) y CountryCurrencyResponse (DTO).
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.CDI,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR
)
public interface CountryCurrencyDtoMapper {

    CountryCurrencyResponse toResponse(CountryCurrency countryCurrency);
}