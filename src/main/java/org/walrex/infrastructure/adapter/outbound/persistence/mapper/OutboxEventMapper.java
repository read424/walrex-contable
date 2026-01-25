package org.walrex.infrastructure.adapter.outbound.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.walrex.domain.model.OutboxEvent;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.OutboxEventEntity;

@Mapper(
        componentModel = MappingConstants.ComponentModel.CDI,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface OutboxEventMapper {
    OutboxEvent toDomain(OutboxEventEntity entity);

    OutboxEventEntity toEntity(OutboxEvent domain);
}
