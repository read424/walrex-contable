package org.walrex.infrastructure.adapter.inbound.mapper;

import org.mapstruct.*;
import org.walrex.application.dto.request.CreateCurrencyRequest;
import org.walrex.application.dto.request.UpdateCurrencyRequest;
import org.walrex.domain.model.Currency;

@Mapper(
        componentModel = MappingConstants.ComponentModel.CDI,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR
)
public interface CurrencyRequestMapper {
    /**
     * Convierte un CreateCurrencyRequest a Currency.
     *
     * El mapeo es directo ya que ambos tienen los mismos campos:
     * - alphabeticCode
     * - numericCode
     * - name
     *
     * @param request DTO de entrada desde el REST handler
     * @return Currency para el caso de uso
     */
    Currency toModel(CreateCurrencyRequest request);

    /**
     * Convierte un UpdateCurrencyRequest a Currency.
     *
     * @param request DTO de entrada desde el REST handler
     * @return Currency para el caso de uso
     */
    Currency toModel(UpdateCurrencyRequest request);
}
