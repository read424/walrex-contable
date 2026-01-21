package org.walrex.infrastructure.adapter.outbound.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.walrex.domain.model.Otp;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.OtpEntity;

@Mapper(
        componentModel = MappingConstants.ComponentModel.CDI,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface OtpMapper {
    Otp toDomain(OtpEntity entity);

    OtpEntity toEntity(Otp domain);
}
