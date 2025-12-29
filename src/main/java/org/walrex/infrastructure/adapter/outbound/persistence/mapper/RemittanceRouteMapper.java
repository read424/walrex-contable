package org.walrex.infrastructure.adapter.outbound.persistence.mapper;

import org.mapstruct.*;
import org.walrex.domain.model.RemittanceRoute;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.RemittanceRouteEntity;

import java.util.List;

/**
 * Mapper entre RemittanceRouteEntity y RemittanceRoute.
 *
 * Mapea los campos anidados de CountryCurrencyEntity a los campos planos de RemittanceRoute.
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.CDI,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR
)
public interface RemittanceRouteMapper {

    /**
     * Convierte RemittanceRouteEntity a RemittanceRoute
     *
     * @param entity La entidad de persistencia
     * @return RemittanceRoute modelo de dominio
     */
    @Mapping(source = "countryCurrencyFrom.id", target = "countryCurrencyFromId")
    @Mapping(source = "countryCurrencyFrom.currency.id", target = "currencyFromId")
    @Mapping(source = "countryCurrencyFrom.currency.alphabeticCode", target = "currencyFromCode")
    @Mapping(source = "countryCurrencyTo.id", target = "countryCurrencyToId")
    @Mapping(source = "countryCurrencyTo.currency.id", target = "currencyToId")
    @Mapping(source = "countryCurrencyTo.currency.alphabeticCode", target = "currencyToCode")
    @Mapping(source = "intermediaryAsset", target = "intermediaryAsset")
    RemittanceRoute toDomain(RemittanceRouteEntity entity);

    /**
     * Convierte una lista de entidades a lista de modelos de dominio
     *
     * @param entities Lista de entidades
     * @return Lista de RemittanceRoute
     */
    List<RemittanceRoute> toDomainList(List<RemittanceRouteEntity> entities);
}
