package org.walrex.infrastructure.adapter.outbound.persistence.mapper;

import org.mapstruct.*;
import org.walrex.domain.model.CountryCurrencyPaymentMethod;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.CountryCurrencyPaymentMethodEntity;

import java.util.List;

/**
 * Mapper entre CountryCurrencyPaymentMethodEntity y CountryCurrencyPaymentMethod domain model
 */
@Mapper(
    componentModel = MappingConstants.ComponentModel.CDI,
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    injectionStrategy = InjectionStrategy.CONSTRUCTOR
)
public interface CountryCurrencyPaymentMethodMapper {

    @Mapping(source = "countryCurrency.id", target = "countryCurrencyId")
    @Mapping(source = "bank.id", target = "bankId")
    @Mapping(source = "bank.namePayBinance", target = "bankPaymentCode")
    @Mapping(source = "bank.detName", target = "bankName")
    CountryCurrencyPaymentMethod toDomain(CountryCurrencyPaymentMethodEntity entity);

    @Mapping(source = "countryCurrencyId", target = "countryCurrency.id")
    @Mapping(source = "bankId", target = "bank.id")
    CountryCurrencyPaymentMethodEntity toEntity(CountryCurrencyPaymentMethod domain);

    List<CountryCurrencyPaymentMethod> toDomainList(List<CountryCurrencyPaymentMethodEntity> entities);
}
