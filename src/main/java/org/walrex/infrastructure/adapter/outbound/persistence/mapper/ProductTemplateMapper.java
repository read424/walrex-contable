package org.walrex.infrastructure.adapter.outbound.persistence.mapper;

import org.mapstruct.*;
import org.walrex.domain.model.ProductTemplate;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.ProductTemplateEntity;

import java.util.List;

/**
 * Mapper entre ProductTemplateEntity (persistencia) y ProductTemplate (dominio).
 *
 * Usa MapStruct para generar automáticamente el código de mapeo en tiempo de compilación.
 *
 * IMPORTANTE: Este mapper NO mapea las propiedades de navegación (category, brand, uom, currency).
 * Solo mapea los IDs. Las propiedades de navegación se usan en el DtoMapper para extraer
 * nombres/códigos cuando la entidad fue cargada con JOIN FETCH.
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
public interface ProductTemplateMapper {

    /**
     * Convierte la entidad de persistencia al modelo de dominio.
     *
     * Ignora las propiedades de navegación (category, brand, uom, currency)
     * ya que solo necesitamos los IDs en el modelo de dominio.
     *
     * @param entity Entidad de persistencia
     * @return ProductTemplate modelo de dominio
     */
    ProductTemplate toDomain(ProductTemplateEntity entity);

    /**
     * Convierte el modelo de dominio a la entidad de persistencia.
     *
     * Ignora las propiedades de navegación ya que se manejan automáticamente
     * por JPA usando los campos de ID.
     *
     * @param domain Modelo de dominio
     * @return ProductTemplateEntity entidad de persistencia
     */
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "brand", ignore = true)
    @Mapping(target = "uom", ignore = true)
    @Mapping(target = "currency", ignore = true)
    ProductTemplateEntity toEntity(ProductTemplate domain);

    /**
     * Convierte una lista de entidades a lista de modelos de dominio.
     *
     * @param entities Lista de entidades de persistencia
     * @return Lista de modelos de dominio
     */
    List<ProductTemplate> toDomainList(List<ProductTemplateEntity> entities);

    /**
     * Convierte una lista de modelos de dominio a lista de entidades.
     *
     * @param domains Lista de modelos de dominio
     * @return Lista de entidades de persistencia
     */
    List<ProductTemplateEntity> toEntityList(List<ProductTemplate> domains);
}
