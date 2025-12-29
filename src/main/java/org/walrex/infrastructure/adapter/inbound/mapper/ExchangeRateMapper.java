package org.walrex.infrastructure.adapter.inbound.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.walrex.application.dto.response.ExchangeRateResponse;
import org.walrex.domain.model.ExchangeCalculation;

/**
 * Mapper para convertir entre modelo de dominio y DTOs de Exchange Rate.
 *
 * Utiliza MapStruct para generación automática de código.
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.CDI,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR
)
public interface ExchangeRateMapper {

    /**
     * Convierte el modelo de dominio a DTO de respuesta.
     *
     * @param calculation Resultado del cálculo de cambio de divisas
     * @return DTO de respuesta con todos los detalles del cálculo
     */
    ExchangeRateResponse toResponse(ExchangeCalculation calculation);
}
