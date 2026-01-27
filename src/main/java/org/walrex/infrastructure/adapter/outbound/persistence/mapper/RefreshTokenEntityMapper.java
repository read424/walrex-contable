package org.walrex.infrastructure.adapter.outbound.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.walrex.domain.model.RefreshToken;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.RefreshTokenEntity;

@Mapper(componentModel = MappingConstants.ComponentModel.CDI, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RefreshTokenEntityMapper {
    RefreshTokenEntity toEntity(RefreshToken refreshToken);
    RefreshToken toDomain(RefreshTokenEntity entity);
}
