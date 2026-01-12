package org.walrex.infrastructure.adapter.inbound.mapper;

import org.mapstruct.*;
import org.walrex.application.dto.response.ProductCategoryUomResponse;
import org.walrex.application.dto.response.ProductCategoryUomSelectResponse;
import org.walrex.domain.model.ProductCategoryUom;

import java.util.List;

/**
 * Mapper entre el modelo de dominio ProductCategoryUom y los DTOs de respuesta.
 *
 * Usa MapStruct para generar automáticamente el código de mapeo en tiempo de compilación.
 *
 * Configuración:
 * - componentModel = "cdi": Integración con CDI de Quarkus (permite @Inject)
 * - unmappedTargetPolicy = IGNORE: Ignora campos no mapeados
 * - injectionStrategy = CONSTRUCTOR: Usa inyección por constructor (mejor para inmutabilidad)
 *
 * Responsabilidades:
 * - Convertir ProductCategoryUom (dominio) → ProductCategoryUomResponse (DTO salida)
 * - Convertir ProductCategoryUom (dominio) → ProductCategoryUomSelectResponse (DTO optimizado)
 * - No exponer deletedAt al exterior
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.CDI,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR
)
public interface ProductCategoryUomDtoMapper {

    /**
     * Convierte el modelo de dominio a DTO de respuesta.
     *
     * Mapeo directo de todos los campos excepto deletedAt que no se expone.
     *
     * @param productCategoryUom Modelo de dominio
     * @return ProductCategoryUomResponse DTO de respuesta
     */
    ProductCategoryUomResponse toResponse(ProductCategoryUom productCategoryUom);

    /**
     * Convierte una lista de ProductCategoryUom a lista de ProductCategoryUomResponse.
     *
     * @param productCategoryUoms Lista de modelos de dominio
     * @return Lista de DTOs de respuesta
     */
    List<ProductCategoryUomResponse> toResponseList(List<ProductCategoryUom> productCategoryUoms);

    /**
     * Convierte el modelo de dominio a DTO de respuesta optimizado para selects.
     *
     * Solo incluye campos esenciales: id, code, name
     *
     * @param productCategoryUom Modelo de dominio
     * @return ProductCategoryUomSelectResponse DTO optimizado para selects
     */
    ProductCategoryUomSelectResponse toSelectResponse(ProductCategoryUom productCategoryUom);

    /**
     * Convierte una lista de ProductCategoryUom a lista de ProductCategoryUomSelectResponse.
     *
     * @param productCategoryUoms Lista de modelos de dominio
     * @return Lista de DTOs optimizados para selects
     */
    List<ProductCategoryUomSelectResponse> toSelectResponseList(List<ProductCategoryUom> productCategoryUoms);
}
