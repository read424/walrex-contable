package org.walrex.infrastructure.adapter.outbound.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import org.walrex.domain.model.Ocupacion;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.OcupacionEntity;

@Mapper(
        componentModel = MappingConstants.ComponentModel.CDI,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface OcupacionPersistenceMapper {

    Ocupacion toDomain(OcupacionEntity entity);

    OcupacionEntity toEntity(Ocupacion domain);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDomain(Ocupacion domain, @MappingTarget OcupacionEntity entity);
}
