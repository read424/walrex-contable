package org.walrex.infrastructure.adapter.outbound.persistence.mapper;

import org.mapstruct.*;
import org.walrex.domain.model.Province;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.ProvinceEntity;

@Mapper(
        componentModel = MappingConstants.ComponentModel.JAKARTA,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        uses = DepartamentMapper.class
)
public interface ProvinceMapper {

    @Mapping(source = "code", target = "codigo")
    @Mapping(source = "created_at", target = "createdAt")
    @Mapping(source = "updated_at", target = "updatedAt")
    ProvinceEntity toEntity(Province province);

    @Mapping(source = "codigo", target = "code")
    @Mapping(source = "createdAt", target = "created_at")
    @Mapping(source = "updatedAt", target = "updated_at")
    Province toDomain(ProvinceEntity entity);
}
