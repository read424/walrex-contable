package org.walrex.infrastructure.adapter.inbound.mapper;

import org.mapstruct.*;
import org.walrex.application.dto.request.CreateProductBrandRequest;
import org.walrex.application.dto.response.ProductBrandResponse;
import org.walrex.domain.model.ProductBrand;

@Mapper(
        componentModel = MappingConstants.ComponentModel.CDI,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR
)
public interface ProductBrandMapper {

    /**
     * Convierte el modelo de dominio a DTO de respuesta.
     *
     * @param productBrand Modelo de dominio
     * @return ProductBrandResponse DTO de respuesta
     */
    ProductBrandResponse toResponse(ProductBrand productBrand);

    /**
     * Convierte el DTO de request a modelo de dominio.
     *
     * @param request DTO de request
     * @return ProductBrand modelo de dominio
     */
    ProductBrand toDomain(CreateProductBrandRequest request);
}
