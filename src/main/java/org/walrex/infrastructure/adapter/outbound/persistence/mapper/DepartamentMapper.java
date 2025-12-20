package org.walrex.infrastructure.adapter.outbound.persistence.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.walrex.domain.model.Departament;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.DepartamentEntity;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.CDI, unmappedTargetPolicy = ReportingPolicy.IGNORE, injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface DepartamentMapper {

    @Mapping(source = "code", target = "codigo")
    @Mapping(source = "name", target = "nombre")
    @Mapping(source = "created_at", target = "createdAt")
    @Mapping(source = "updated_at", target = "udpdatedAt")
    DepartamentEntity toEntity(Departament domain);

    @Mapping(source = "codigo", target = "code")
    @Mapping(source = "nombre", target = "name")
    @Mapping(source = "createdAt", target = "created_at")
    @Mapping(source = "udpdatedAt", target = "updated_at")
    Departament toDomain(DepartamentEntity entity);

    List<Departament> toDomainList(List<DepartamentEntity> entities);

    List<DepartamentEntity> toEntityList(List<Departament> domains);
}
