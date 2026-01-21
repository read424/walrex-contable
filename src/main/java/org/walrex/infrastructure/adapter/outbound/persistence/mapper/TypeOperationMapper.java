package org.walrex.infrastructure.adapter.outbound.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.walrex.domain.model.TypeOperation;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.TypeOperationEntity;
import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.CDI)
public interface TypeOperationMapper {
    TypeOperation toDomain(TypeOperationEntity entity);
    TypeOperationEntity toEntity(TypeOperation domain);
    List<TypeOperation> toDomainList(List<TypeOperationEntity> entities);
    List<TypeOperationEntity> toEntityList(List<TypeOperation> domains);
}
