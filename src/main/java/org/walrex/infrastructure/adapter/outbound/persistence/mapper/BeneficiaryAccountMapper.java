package org.walrex.infrastructure.adapter.outbound.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.walrex.domain.model.BeneficiaryAccount;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.BeneficiaryAccountEntity;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.CDI)
public interface BeneficiaryAccountMapper {
    BeneficiaryAccount toDomain(BeneficiaryAccountEntity entity);
    BeneficiaryAccountEntity toEntity(BeneficiaryAccount domain);
    List<BeneficiaryAccount> toDomainList(List<BeneficiaryAccountEntity> entities);
    List<BeneficiaryAccountEntity> toEntityList(List<BeneficiaryAccount> domains);
}
