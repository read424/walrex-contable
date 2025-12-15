package org.walrex.infrastructure.adapter.inbound.mapper;

import org.mapstruct.*;
import org.walrex.application.dto.request.CreateSystemDocumentTypeRequest;
import org.walrex.application.dto.request.UpdateSystemDocumentTypeRequest;
import org.walrex.domain.model.SystemDocumentType;

/**
 * Mapper para convertir DTOs de request a modelo de dominio.
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.CDI,
        unmappedTargetPolicy =  ReportingPolicy.IGNORE,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR
)
public interface SystemDocumentTypeRequestMapper {

    /**
     * Convierte CreateSystemDocumentTypeRequest a modelo de dominio.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    SystemDocumentType toModel(CreateSystemDocumentTypeRequest request);

    /**
     * Convierte UpdateSystemDocumentTypeRequest a modelo de dominio.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    SystemDocumentType toModel(UpdateSystemDocumentTypeRequest request);
}
