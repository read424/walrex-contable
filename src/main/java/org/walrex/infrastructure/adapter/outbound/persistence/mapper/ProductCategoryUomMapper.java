package org.walrex.infrastructure.adapter.outbound.persistence.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.walrex.domain.model.ProductCategoryUom;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.ProductCategoryUomEntity;

import java.util.List;

/**
 * Mapper entre el modelo de dominio ProductCategoryUom y la entidad de persistencia ProductCategoryUomEntity.
 *
 * Usa MapStruct para generar automáticamente el código de mapeo en tiempo de compilación.
 *
 * Siguiendo el patrón hexagonal, este mapper pertenece a la capa de infraestructura
 * ya que conoce tanto el modelo de dominio como los detalles de persistencia.
 *
 * Configuración:
 * - componentModel = "cdi": Integración con CDI de Quarkus (permite @Inject)
 * - unmappedTargetPolicy = IGNORE: Ignora campos no mapeados
 * - injectionStrategy = CONSTRUCTOR: Usa inyección por constructor
 *
 * Nota: Como todos los campos tienen el mismo nombre en ProductCategoryUom y ProductCategoryUomEntity,
 * MapStruct puede hacer el mapeo automáticamente sin necesidad de @Mapping.
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.CDI,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR
)
public interface ProductCategoryUomMapper {

    /**
     * Convierte una entidad de dominio a una entidad de persistencia.
     *
     * MapStruct mapea automáticamente todos los campos porque tienen el mismo nombre:
     * - id → id
     * - code → code
     * - name → name
     * - description → description
     * - active → active
     * - createdAt → createdAt
     * - updatedAt → updatedAt
     * - deletedAt → deletedAt
     *
     * @param domain Modelo de dominio ProductCategoryUom
     * @return ProductCategoryUomEntity para persistencia
     */
    ProductCategoryUomEntity toEntity(ProductCategoryUom domain);

    /**
     * Convierte una entidad de persistencia a un modelo de dominio.
     *
     * MapStruct mapea automáticamente todos los campos porque tienen el mismo nombre.
     *
     * @param entity ProductCategoryUomEntity de persistencia
     * @return ProductCategoryUom modelo de dominio
     */
    ProductCategoryUom toDomain(ProductCategoryUomEntity entity);

    /**
     * Convierte una lista de entidades de persistencia a lista de modelos de dominio.
     *
     * @param entities Lista de ProductCategoryUomEntity
     * @return Lista de ProductCategoryUom
     */
    List<ProductCategoryUom> toDomainList(List<ProductCategoryUomEntity> entities);

    /**
     * Convierte una lista de modelos de dominio a lista de entidades de persistencia.
     *
     * @param domains Lista de ProductCategoryUom
     * @return Lista de ProductCategoryUomEntity
     */
    List<ProductCategoryUomEntity> toEntityList(List<ProductCategoryUom> domains);
}
