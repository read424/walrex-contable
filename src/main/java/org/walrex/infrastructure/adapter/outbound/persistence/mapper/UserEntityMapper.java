package org.walrex.infrastructure.adapter.outbound.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.walrex.domain.model.User;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.UserEntity;

@Mapper(componentModel = MappingConstants.ComponentModel.CDI, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserEntityMapper {

    @Mapping(target = "clientId", source = "customerId")
    UserEntity toEntity(User user);

    @Mapping(source = "clientId", target = "customerId")
    User toDomain(UserEntity entity);
}
