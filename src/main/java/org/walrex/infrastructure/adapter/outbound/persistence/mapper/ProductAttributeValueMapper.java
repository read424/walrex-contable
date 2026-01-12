package org.walrex.infrastructure.adapter.outbound.persistence.mapper;

import org.mapstruct.*;
import org.walrex.domain.model.ProductAttributeValue;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.ProductAttributeValueEntity;

import java.util.List;

/**
 * Mapper entre la entidad de persistencia ProductAttributeValueEntity y el modelo de dominio ProductAttributeValue.
 *
 * Usa MapStruct para generar automáticamente el código de mapeo en tiempo de compilación.
 *
 * IMPORTANTE: Este mapper ignora el objeto 'attribute' de la entidad.
 * Solo mapea attributeId, no el objeto completo.
 *
 * Configuración:
 * - componentModel = "cdi": Integración con CDI de Quarkus (permite @Inject)
 * - unmappedTargetPolicy = IGNORE: Ignora campos no mapeados
 * - injectionStrategy = CONSTRUCTOR: Usa inyección por constructor (mejor para inmutabilidad)
 *
 * Responsabilidades:
 * - Convertir ProductAttributeValueEntity (JPA) ↔ ProductAttributeValue (dominio)
 * - Ignorar el objeto 'attribute' (relación @ManyToOne)
 * - Mapear todos los demás campos directamente
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.CDI,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR
)
public interface ProductAttributeValueMapper {

    /**
     * Convierte la entidad de persistencia a modelo de dominio.
     *
     * @param entity Entidad de persistencia
     * @return ProductAttributeValue Modelo de dominio
     */
    ProductAttributeValue toDomain(ProductAttributeValueEntity entity);

    /**
     * Convierte una lista de entidades a lista de modelos de dominio.
     *
     * @param entities Lista de entidades de persistencia
     * @return Lista de modelos de dominio
     */
    List<ProductAttributeValue> toDomainList(List<ProductAttributeValueEntity> entities);

    /**
     * Convierte el modelo de dominio a entidad de persistencia.
     *
     * @param productAttributeValue Modelo de dominio
     * @return ProductAttributeValueEntity Entidad de persistencia
     */
    @Mapping(target = "attribute", ignore = true)
    ProductAttributeValueEntity toEntity(ProductAttributeValue productAttributeValue);

    /**
     * Convierte una lista de modelos de dominio a lista de entidades.
     *
     * @param productAttributeValues Lista de modelos de dominio
     * @return Lista de entidades de persistencia
     */
    List<ProductAttributeValueEntity> toEntityList(List<ProductAttributeValue> productAttributeValues);
}
