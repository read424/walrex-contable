package org.walrex.infrastructure.adapter.outbound.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.walrex.domain.model.AccountWallet;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.AccountWalletEntity;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.CDI, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AccountWalletEntityMapper {

    AccountWalletEntity toEntity(AccountWallet accountWallet);

    AccountWallet toDomain(AccountWalletEntity entity);

    List<AccountWallet> toDomainList(List<AccountWalletEntity> entities);

    List<AccountWalletEntity> toEntityList(List<AccountWallet> accountWallets);
}
