package org.walrex.infrastructure.adapter.outbound.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.walrex.domain.model.Customer;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.CustomerEntity;

@Mapper(componentModel = MappingConstants.ComponentModel.CDI, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CustomerEntityMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", expression = "java(java.time.OffsetDateTime.now())")
    @Mapping(source = "idProfessional", target = "idProfessional")
    @Mapping(source = "isPEP", target = "isPEP")
    CustomerEntity toEntity(Customer customer);

    @Mapping(source = "isPEP", target = "isPEP")
    @Mapping(source = "idProfessional", target = "idProfessional")
    Customer toDomain(CustomerEntity entity);
}
