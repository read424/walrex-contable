package org.walrex.infrastructure.adapter.outbound.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.walrex.domain.model.Beneficiary;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.BeneficiaryEntity;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.CDI)
public interface BeneficiaryMapper {
    Beneficiary toDomain(BeneficiaryEntity entity);
    BeneficiaryEntity toEntity(Beneficiary domain);
    List<Beneficiary> toDomainList(List<BeneficiaryEntity> entities);
    List<BeneficiaryEntity> toEntityList(List<Beneficiary> domains);
}
