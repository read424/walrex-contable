package org.walrex.infrastructure.adapter.outbound.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.walrex.domain.model.WalletTransaction;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.WalletTransactionEntity;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.CDI, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface WalletTransactionEntityMapper {

    WalletTransactionEntity toEntity(WalletTransaction walletTransaction);

    WalletTransaction toDomain(WalletTransactionEntity entity);

    List<WalletTransaction> toDomainList(List<WalletTransactionEntity> entities);
}
