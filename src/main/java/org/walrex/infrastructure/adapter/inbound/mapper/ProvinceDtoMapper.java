package org.walrex.infrastructure.adapter.inbound.mapper;

import org.mapstruct.*;
import org.walrex.application.dto.response.ProvinceResponse;
import org.walrex.domain.model.Province;

import java.util.List;

@Mapper(
        componentModel = MappingConstants.ComponentModel.JAKARTA,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        uses = DepartamentDtoMapper.class
)
public interface ProvinceDtoMapper {
    @Mapping(source = "code", target = "codigo")
    ProvinceResponse toResponse(Province domain);

    List<ProvinceResponse> toResponseList(List<Province> domain);
}
