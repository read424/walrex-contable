package org.walrex.infrastructure.adapter.outbound.persistence.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.walrex.application.dto.response.DepartamentResponse;
import org.walrex.application.dto.response.DistrictResponse;
import org.walrex.application.dto.response.ProvinceResponse;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.DepartamentEntity;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.DistrictEntity;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.ProvinceEntity;

/**
 * Mapper entre entidades de persistencia de Ubigeo y DTOs de respuesta.
 *
 * Usa MapStruct para generar automáticamente el código de mapeo en tiempo de compilación.
 *
 * Este mapper maneja la jerarquía completa de Ubigeo:
 * - DistrictEntity → DistrictResponse (con ProvinceResponse anidado)
 * - ProvinceEntity → ProvinceResponse (con DepartamentResponse anidado)
 * - DepartamentEntity → DepartamentResponse
 *
 * Gracias al EAGER loading configurado en las entidades, cuando se mapea un DistrictEntity,
 * automáticamente se mapean también su ProvinceEntity y DepartamentEntity asociados.
 *
 * Configuración:
 * - componentModel = "cdi": Integración con CDI de Quarkus (permite @Inject)
 * - unmappedTargetPolicy = IGNORE: Ignora campos no mapeados
 * - injectionStrategy = CONSTRUCTOR: Usa inyección por constructor
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.CDI,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR
)
public interface UbigeoResponseMapper {

    /**
     * Convierte una entidad de Departamento a DTO de respuesta.
     *
     * @param entity DepartamentEntity de persistencia
     * @return DepartamentResponse DTO
     */
    DepartamentResponse toResponse(DepartamentEntity entity);

    /**
     * Convierte una entidad de Provincia a DTO de respuesta.
     *
     * MapStruct automáticamente mapea la relación anidada:
     * - departament (DepartamentEntity) → departament (DepartamentResponse)
     *
     * @param entity ProvinceEntity de persistencia
     * @return ProvinceResponse DTO con DepartamentResponse anidado
     */
    ProvinceResponse toResponse(ProvinceEntity entity);

    /**
     * Convierte una entidad de Distrito a DTO de respuesta.
     *
     * MapStruct automáticamente mapea toda la jerarquía:
     * - province (ProvinceEntity) → province (ProvinceResponse)
     *   - departament (DepartamentEntity) → departament (DepartamentResponse)
     *
     * Gracias al EAGER loading, todas las entidades relacionadas ya están cargadas.
     *
     * @param entity DistrictEntity de persistencia
     * @return DistrictResponse DTO con toda la jerarquía anidada
     */
    DistrictResponse toResponse(DistrictEntity entity);
}
