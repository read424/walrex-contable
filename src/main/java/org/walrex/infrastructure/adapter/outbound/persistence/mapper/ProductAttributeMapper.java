package org.walrex.infrastructure.adapter.outbound.persistence.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.walrex.domain.model.ProductAttribute;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.ProductAttributeEntity;

import java.util.List;

/**
 * Mapper entre el modelo de dominio ProductAttribute y la entidad de persistencia ProductAttributeEntity.
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
 * Nota: Como todos los campos tienen el mismo nombre en ProductAttribute y ProductAttributeEntity,
 * MapStruct puede hacer el mapeo automáticamente sin necesidad de @Mapping.
 * El enum AttributeDisplayType se mapea automáticamente porque el converter JPA se encarga de la conversión.
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.CDI,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR
)
public interface ProductAttributeMapper {

    /**
     * Convierte una entidad de dominio a una entidad de persistencia.
     *
     * MapStruct mapea automáticamente todos los campos porque tienen el mismo nombre:
     * - id → id
     * - name → name
     * - displayType → displayType (enum se convierte automáticamente)
     * - active → active
     * - createdAt → createdAt
     * - updatedAt → updatedAt
     * - deletedAt → deletedAt
     *
     * @param domain Modelo de dominio ProductAttribute
     * @return ProductAttributeEntity para persistencia
     */
    ProductAttributeEntity toEntity(ProductAttribute domain);

    /**
     * Convierte una entidad de persistencia a un modelo de dominio.
     *
     * MapStruct mapea automáticamente todos los campos porque tienen el mismo nombre.
     *
     * @param entity ProductAttributeEntity de persistencia
     * @return ProductAttribute modelo de dominio
     */
    ProductAttribute toDomain(ProductAttributeEntity entity);

    /**
     * Convierte una lista de entidades de persistencia a lista de modelos de dominio.
     *
     * @param entities Lista de ProductAttributeEntity
     * @return Lista de ProductAttribute
     */
    List<ProductAttribute> toDomainList(List<ProductAttributeEntity> entities);

    /**
     * Convierte una lista de modelos de dominio a lista de entidades de persistencia.
     *
     * @param domains Lista de ProductAttribute
     * @return Lista de ProductAttributeEntity
     */
    List<ProductAttributeEntity> toEntityList(List<ProductAttribute> domains);
}
