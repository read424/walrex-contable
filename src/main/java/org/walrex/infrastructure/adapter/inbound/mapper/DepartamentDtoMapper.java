package org.walrex.infrastructure.adapter.inbound.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.walrex.application.dto.response.DepartamentResponse;
import org.walrex.domain.model.Departament;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.CDI, unmappedTargetPolicy = ReportingPolicy.IGNORE, injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface DepartamentDtoMapper {

    @Mapping(source = "code", target = "codigo")
    @Mapping(source = "name", target = "nombre")
    DepartamentResponse toResponse(Departament domain);

    List<DepartamentResponse> toResponseList(List<Departament> domains);
}
