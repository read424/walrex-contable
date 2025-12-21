package org.walrex.infrastructure.adapter.inbound.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.walrex.application.dto.request.CreateProvinceRequest;
import org.walrex.application.dto.request.UpdateProvinceRequest;
import org.walrex.domain.model.Departament;
import org.walrex.domain.model.Province;

@Mapper(
        componentModel = MappingConstants.ComponentModel.JAKARTA,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR
)
public interface ProvinceRequestMapper {

    @Mapping(source = "nombre", target = "name")
    @Mapping(source = "idDepartamento", target = "departament.id")
    Province toModel(CreateProvinceRequest request);

    @Mapping(source = "nombre", target = "name")
    Province toModel(UpdateProvinceRequest request);
}
