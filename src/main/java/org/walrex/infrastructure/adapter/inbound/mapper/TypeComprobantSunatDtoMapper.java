package org.walrex.infrastructure.adapter.inbound.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.walrex.application.dto.response.TypeComprobantSunatSelectResponse;
import org.walrex.domain.model.TypeComprobantSunat;

import java.util.List;

@Mapper(
        componentModel = MappingConstants.ComponentModel.CDI,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR
)
public interface TypeComprobantSunatDtoMapper {
    /**
     * Convierte modelo de dominio a TypeComprobantSunatSelectResponse (optimizado para selects).
     */
    TypeComprobantSunatSelectResponse toSelectResponse(TypeComprobantSunat domain);

    /**
     * Convierte lista de modelos de dominio a lista de select responses.
     */
    List<TypeComprobantSunatSelectResponse> toSelectResponseList(List<TypeComprobantSunat> domains);
}
