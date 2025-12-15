package org.walrex.infrastructure.adapter.inbound.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.walrex.application.dto.response.SystemDocumentTypeResponse;
import org.walrex.domain.model.SystemDocumentType;

import java.util.List;

/**
 * Mapper para convertir modelo de dominio a DTOs de respuesta.
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.CDI,
        unmappedTargetPolicy =  ReportingPolicy.IGNORE,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR
)
public interface SystemDocumentTypeDtoMapper {

    /**
     * Convierte modelo de dominio a SystemDocumentTypeResponse.
     */
    SystemDocumentTypeResponse toResponse(SystemDocumentType domain);

    /**
     * Convierte lista de modelos de dominio a lista de responses.
     */
    List<SystemDocumentTypeResponse> toResponseList(List<SystemDocumentType> domains);
}
