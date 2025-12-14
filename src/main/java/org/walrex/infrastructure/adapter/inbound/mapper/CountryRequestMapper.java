package org.walrex.infrastructure.adapter.inbound.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.walrex.application.dto.request.CreateCountryRequest;
import org.walrex.application.dto.request.UpdateCountryRequest;
import org.walrex.domain.model.Country;

@Mapper(
        componentModel = MappingConstants.ComponentModel.CDI,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR
)
public interface CountryRequestMapper {
    /**
     * Convierte un CreateCountryRequest a Country.
     *
     * El mapeo es directo ya que ambos tienen los mismos campos:
     * - alphabeticCode
     * - numericCode
     * - name
     *
     * @param request DTO de entrada desde el REST handler
     * @return Country para el caso de uso
     */
    @Mapping(source = "alphabeticCode", target = "alphabeticCode3")
    Country toModel(CreateCountryRequest request);

    /**
     * Convierte un UpdateCountryRequest a Country.
     *
     * @param request DTO de entrada desde el REST handler
     * @return Country para el caso de uso
     */
    @Mapping(source = "alphabeticCode", target = "alphabeticCode3")
    Country toModel(UpdateCountryRequest request);
}
