package org.walrex.infrastructure.adapter.inbound.mapper;

import org.mapstruct.*;
import org.walrex.application.dto.response.ProductAttributeValueResponse;
import org.walrex.application.dto.response.ProductAttributeValueSelectResponse;
import org.walrex.domain.model.ProductAttributeValue;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.ProductAttributeValueEntity;

import java.util.List;

/**
 * Mapper entre el modelo de dominio ProductAttributeValue y los DTOs de respuesta.
 *
 * Usa MapStruct para generar automáticamente el código de mapeo en tiempo de compilación.
 *
 * IMPORTANTE: Este mapper también puede convertir desde ProductAttributeValueEntity
 * para extraer la información del atributo relacionado (attributeName, attributeDisplayType)
 * cuando la entidad tiene el objeto attribute cargado con JOIN FETCH.
 *
 * Configuración:
 * - componentModel = "cdi": Integración con CDI de Quarkus (permite @Inject)
 * - unmappedTargetPolicy = IGNORE: Ignora campos no mapeados
 * - injectionStrategy = CONSTRUCTOR: Usa inyección por constructor (mejor para inmutabilidad)
 *
 * Responsabilidades:
 * - Convertir ProductAttributeValue (dominio) → ProductAttributeValueResponse (DTO salida)
 * - Convertir ProductAttributeValueEntity (persistencia) → ProductAttributeValueResponse (con info de atributo)
 * - Convertir ProductAttributeValue (dominio) → ProductAttributeValueSelectResponse (DTO optimizado)
 * - No exponer deletedAt al exterior
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.CDI,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR
)
public interface ProductAttributeValueDtoMapper {

    /**
     * Convierte el modelo de dominio a DTO de respuesta.
     *
     * NOTA: Este método no puede incluir attributeName ni attributeDisplayType
     * porque el modelo de dominio solo tiene attributeId.
     * Use toResponseFromEntity si necesita esos campos.
     *
     * @param productAttributeValue Modelo de dominio
     * @return ProductAttributeValueResponse DTO de respuesta (attributeName y attributeDisplayType serán null)
     */
    @Mapping(target = "attributeName", ignore = true)
    @Mapping(target = "attributeDisplayType", ignore = true)
    ProductAttributeValueResponse toResponse(ProductAttributeValue productAttributeValue);

    /**
     * Convierte la entidad de persistencia a DTO de respuesta.
     *
     * Este método extrae attributeName y attributeDisplayType del objeto attribute relacionado
     * si fue cargado con JOIN FETCH. Si attribute es null, esos campos serán null.
     *
     * @param entity Entidad de persistencia (idealmente con attribute cargado)
     * @return ProductAttributeValueResponse DTO de respuesta con información completa
     */
    @Mapping(source = "attribute.name", target = "attributeName")
    @Mapping(source = "attribute.displayType", target = "attributeDisplayType")
    ProductAttributeValueResponse toResponseFromEntity(ProductAttributeValueEntity entity);

    /**
     * Convierte una lista de ProductAttributeValueEntity a lista de ProductAttributeValueResponse.
     *
     * @param entities Lista de entidades de persistencia
     * @return Lista de DTOs de respuesta
     */
    List<ProductAttributeValueResponse> toResponseListFromEntities(List<ProductAttributeValueEntity> entities);

    /**
     * Convierte el modelo de dominio a DTO de respuesta optimizado para selects.
     *
     * Solo incluye campos esenciales: id, name, htmlColor
     *
     * @param productAttributeValue Modelo de dominio
     * @return ProductAttributeValueSelectResponse DTO optimizado para selects
     */
    ProductAttributeValueSelectResponse toSelectResponse(ProductAttributeValue productAttributeValue);

    /**
     * Convierte una lista de ProductAttributeValue a lista de ProductAttributeValueSelectResponse.
     *
     * @param productAttributeValues Lista de modelos de dominio
     * @return Lista de DTOs optimizados para selects
     */
    List<ProductAttributeValueSelectResponse> toSelectResponseList(List<ProductAttributeValue> productAttributeValues);
}
