package org.walrex.infrastructure.adapter.inbound.mapper;

import org.mapstruct.*;
import org.walrex.application.dto.response.ProductUomResponse;
import org.walrex.application.dto.response.ProductUomSelectResponse;
import org.walrex.domain.model.ProductUom;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.ProductUomEntity;

import java.util.List;

/**
 * Mapper entre el modelo de dominio ProductUom y los DTOs de respuesta.
 *
 * Usa MapStruct para generar automáticamente el código de mapeo en tiempo de compilación.
 *
 * IMPORTANTE: Este mapper también puede convertir desde ProductUomEntity
 * para extraer la información de la categoría relacionada (categoryCode, categoryName)
 * cuando la entidad tiene el objeto category cargado con JOIN FETCH.
 *
 * Configuración:
 * - componentModel = "cdi": Integración con CDI de Quarkus (permite @Inject)
 * - unmappedTargetPolicy = IGNORE: Ignora campos no mapeados
 * - injectionStrategy = CONSTRUCTOR: Usa inyección por constructor (mejor para inmutabilidad)
 *
 * Responsabilidades:
 * - Convertir ProductUom (dominio) → ProductUomResponse (DTO salida)
 * - Convertir ProductUomEntity (persistencia) → ProductUomResponse (con info de categoría)
 * - Convertir ProductUom (dominio) → ProductUomSelectResponse (DTO optimizado)
 * - No exponer deletedAt al exterior
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.CDI,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR
)
public interface ProductUomDtoMapper {

    /**
     * Convierte el modelo de dominio a DTO de respuesta.
     *
     * NOTA: Este método no puede incluir categoryCode ni categoryName
     * porque el modelo de dominio solo tiene categoryId.
     * Use toResponseFromEntity si necesita esos campos.
     *
     * @param productUom Modelo de dominio
     * @return ProductUomResponse DTO de respuesta (categoryCode y categoryName serán null)
     */
    @Mapping(target = "categoryCode", ignore = true)
    @Mapping(target = "categoryName", ignore = true)
    ProductUomResponse toResponse(ProductUom productUom);

    /**
     * Convierte la entidad de persistencia a DTO de respuesta.
     *
     * Este método extrae categoryCode y categoryName del objeto category relacionado
     * si fue cargado con JOIN FETCH. Si category es null, esos campos serán null.
     *
     * @param entity Entidad de persistencia (idealmente con category cargado)
     * @return ProductUomResponse DTO de respuesta con información completa
     */
    @Mapping(source = "category.code", target = "categoryCode")
    @Mapping(source = "category.name", target = "categoryName")
    ProductUomResponse toResponseFromEntity(ProductUomEntity entity);

    /**
     * Convierte una lista de ProductUomEntity a lista de ProductUomResponse.
     *
     * @param entities Lista de entidades de persistencia
     * @return Lista de DTOs de respuesta
     */
    List<ProductUomResponse> toResponseListFromEntities(List<ProductUomEntity> entities);

    /**
     * Convierte el modelo de dominio a DTO de respuesta optimizado para selects.
     *
     * Solo incluye campos esenciales: id, codeUom, nameUom
     *
     * @param productUom Modelo de dominio
     * @return ProductUomSelectResponse DTO optimizado para selects
     */
    ProductUomSelectResponse toSelectResponse(ProductUom productUom);

    /**
     * Convierte una lista de ProductUom a lista de ProductUomSelectResponse.
     *
     * @param productUoms Lista de modelos de dominio
     * @return Lista de DTOs optimizados para selects
     */
    List<ProductUomSelectResponse> toSelectResponseList(List<ProductUom> productUoms);
}
