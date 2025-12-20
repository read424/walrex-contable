package org.walrex.infrastructure.adapter.inbound.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.walrex.application.dto.request.CreateDepartamentRequest;
import org.walrex.application.dto.request.UpdateDepartamentRequest;
import org.walrex.domain.model.Departament;

@Mapper(componentModel = MappingConstants.ComponentModel.CDI, unmappedTargetPolicy = ReportingPolicy.IGNORE, injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface DepartamentRequestMapper {

    @Mapping(source = "nombre", target = "name")
    Departament toModel(CreateDepartamentRequest request);

    @Mapping(source = "codigo", target = "code")
    @Mapping(source = "nombre", target = "name")
    Departament toModel(UpdateDepartamentRequest request);
}
