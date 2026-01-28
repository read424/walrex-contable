package org.walrex.infrastructure.adapter.outbound.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.walrex.domain.model.WalletCountryConfig;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.WalletCountryConfigEntity;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.CDI, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface WalletCountryConfigEntityMapper {

    WalletCountryConfigEntity toEntity(WalletCountryConfig walletCountryConfig);

    WalletCountryConfig toDomain(WalletCountryConfigEntity entity);

    List<WalletCountryConfig> toDomainList(List<WalletCountryConfigEntity> entities);
}
