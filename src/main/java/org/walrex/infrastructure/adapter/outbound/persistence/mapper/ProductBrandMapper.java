package org.walrex.infrastructure.adapter.outbound.persistence.mapper;

import org.mapstruct.*;
import org.walrex.domain.model.ProductBrand;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.ProductBrandEntity;

@Mapper(
        componentModel = MappingConstants.ComponentModel.CDI,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR
)
public interface ProductBrandMapper {

    /**
     * Convierte una entidad de dominio a una entidad de persistencia.
     *
     * MapStruct mapea automáticamente todos los campos porque tienen el mismo nombre:
     * - id → id
     * - name → name
     * - details → details
     * - createdAt → createdAt
     *
     * @param domain Modelo de dominio ProductBrand
     * @return ProductBrandEntity para persistencia
     */
    ProductBrandEntity toEntity(ProductBrand domain);

    /**
     * Convierte una entidad de persistencia a una entidad de dominio.
     *
     * @param entity Entidad de persistencia
     * @return ProductBrand modelo de dominio
     */
    ProductBrand toDomain(ProductBrandEntity entity);
}
