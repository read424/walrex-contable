package org.walrex.infrastructure.adapter.inbound.mapper;

import org.mapstruct.*;
import org.walrex.application.dto.response.ProductAttributeResponse;
import org.walrex.application.dto.response.ProductAttributeSelectResponse;
import org.walrex.domain.model.ProductAttribute;

import java.util.List;

/**
 * Mapper entre el modelo de dominio ProductAttribute y los DTOs de respuesta.
 *
 * Usa MapStruct para generar automáticamente el código de mapeo en tiempo de compilación.
 *
 * Configuración:
 * - componentModel = "cdi": Integración con CDI de Quarkus (permite @Inject)
 * - unmappedTargetPolicy = IGNORE: Ignora campos no mapeados
 * - injectionStrategy = CONSTRUCTOR: Usa inyección por constructor (mejor para inmutabilidad)
 *
 * Responsabilidades:
 * - Convertir ProductAttribute (dominio) → ProductAttributeResponse (DTO salida)
 * - Convertir ProductAttribute (dominio) → ProductAttributeSelectResponse (DTO optimizado)
 * - No exponer deletedAt al exterior
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.CDI,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR
)
public interface ProductAttributeDtoMapper {

    /**
     * Convierte el modelo de dominio a DTO de respuesta.
     *
     * Mapeo directo de todos los campos excepto deletedAt que no se expone.
     *
     * @param productAttribute Modelo de dominio
     * @return ProductAttributeResponse DTO de respuesta
     */
    ProductAttributeResponse toResponse(ProductAttribute productAttribute);

    /**
     * Convierte una lista de ProductAttribute a lista de ProductAttributeResponse.
     *
     * @param productAttributes Lista de modelos de dominio
     * @return Lista de DTOs de respuesta
     */
    List<ProductAttributeResponse> toResponseList(List<ProductAttribute> productAttributes);

    /**
     * Convierte el modelo de dominio a DTO de respuesta optimizado para selects.
     *
     * Solo incluye campos esenciales: id, name
     *
     * @param productAttribute Modelo de dominio
     * @return ProductAttributeSelectResponse DTO optimizado para selects
     */
    ProductAttributeSelectResponse toSelectResponse(ProductAttribute productAttribute);

    /**
     * Convierte una lista de ProductAttribute a lista de ProductAttributeSelectResponse.
     *
     * @param productAttributes Lista de modelos de dominio
     * @return Lista de DTOs optimizados para selects
     */
    List<ProductAttributeSelectResponse> toSelectResponseList(List<ProductAttribute> productAttributes);
}
