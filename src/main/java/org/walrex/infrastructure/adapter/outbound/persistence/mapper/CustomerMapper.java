package org.walrex.infrastructure.adapter.outbound.persistence.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import org.walrex.domain.model.Customer;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.CustomerEntity;

import java.util.List;

/**
 * Mapper entre el modelo de dominio Customer y la entidad de persistencia
 * CustomerEntity.
 *
 * Usa MapStruct para generar automáticamente el código de mapeo en tiempo de
 * compilación.
 *
 * Siguiendo el patrón hexagonal, este mapper pertenece a la capa de
 * infraestructura
 * ya que conoce tanto el modelo de dominio como los detalles de persistencia.
 *
 * Configuración:
 * - componentModel = "cdi": Integración con CDI de Quarkus (permite @Inject)
 * - unmappedTargetPolicy = IGNORE: Ignora campos no mapeados
 * - injectionStrategy = CONSTRUCTOR: Usa inyección por constructor
 *
 * Nota: Como todos los campos tienen el mismo nombre en Customer y
 * CustomerEntity,
 * MapStruct puede hacer el mapeo automáticamente sin necesidad de @Mapping.
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.CDI,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR
)
public interface CustomerMapper {

    /**
     * Convierte una entidad de dominio a una entidad de persistencia.
     *
     * MapStruct mapea automáticamente todos los campos porque tienen el mismo
     * nombre.
     *
     * @param domain Modelo de dominio Customer
     * @return CustomerEntity para persistencia
     */
    CustomerEntity toEntity(Customer domain);

    /**
     * Convierte una entidad de persistencia a un modelo de dominio.
     *
     * MapStruct mapea automáticamente todos los campos porque tienen el mismo
     * nombre.
     *
     * @param entity CustomerEntity de persistencia
     * @return Customer modelo de dominio
     */
    Customer toDomain(CustomerEntity entity);

    /**
     * Convierte una lista de entidades de persistencia a lista de modelos de
     * dominio.
     *
     * @param entities Lista de CustomerEntity
     * @return Lista de Customer
     */
    List<Customer> toDomainList(List<CustomerEntity> entities);

    /**
     * Convierte una lista de modelos de dominio a lista de entidades de
     * persistencia.
     *
     * @param domains Lista de Customer
     * @return Lista de CustomerEntity
     */
    List<CustomerEntity> toEntityList(List<Customer> domains);

    /**
     * Actualiza una entidad de persistencia existente con datos del modelo de
     * dominio.
     *
     * @param domain Modelo de dominio con nuevos datos
     * @param entity Entidad de persistencia a actualizar
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    void updateEntityFromDomain(Customer domain, @MappingTarget CustomerEntity entity);
}
