package org.walrex.infrastructure.adapter.outbound.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.walrex.domain.model.DeviceToken;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.DeviceTokenEntity;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DeviceTokenEntityMapper {

    DeviceTokenEntity toEntity(DeviceToken domain);

    DeviceToken toDomain(DeviceTokenEntity entity);

    List<DeviceToken> toDomainList(List<DeviceTokenEntity> entities);
}
