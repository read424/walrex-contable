package org.walrex.infrastructure.adapter.outbound.persistence.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.walrex.domain.model.Bank;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.BankEntity;

import java.util.List;

/**
 * Mapper entre BankEntity y Bank domain model
 */
@Mapper(
    componentModel = MappingConstants.ComponentModel.CDI,
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    injectionStrategy = InjectionStrategy.CONSTRUCTOR
)
public interface BankMapper {

    Bank toDomain(BankEntity entity);

    BankEntity toEntity(Bank domain);

    List<Bank> toDomainList(List<BankEntity> entities);
}
