package org.walrex.infrastructure.adapter.outbound.persistence.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.walrex.domain.model.Country;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.CountryEntity;

import java.util.List;

/**
 * Mapper entre el modelo de dominio Country y la entidad de persistencia CountryEntity.
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
 * Nota: Como todos los campos tienen el mismo nombre en Currency y CurrencyEntity,
 * MapStruct puede hacer el mapeo automáticamente sin necesidad de @Mapping.
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.CDI,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR
)
public interface CountryMapper {
    /**
     * Convierte una entidad de dominio a una entidad de persistencia.
     *
     * MapStruct mapea automáticamente todos los campos porque tienen el mismo nombre:
     * - id → id
     * - alphabeticCode → alphabeticCode
     * - numericCode → numericCode
     * - name → name
     * - status → status
     * - createdAt → createdAt
     * - updatedAt → updatedAt
     * - deletedAt → deletedAt
     *
     * @param domain Modelo de dominio Country
     * @return CountryEntity para persistencia
     */
    CountryEntity toEntity(Country domain);

    /**
     * Convierte una entidad de persistencia a un modelo de dominio.
     *
     * MapStruct mapea automáticamente todos los campos porque tienen el mismo nombre.
     *
     * @param entity CountryEntity de persistencia
     * @return Country modelo de dominio
     */
    Country toDomain(CountryEntity entity);

    /**
     * Convierte una lista de entidades de persistencia a lista de modelos de dominio.
     *
     * @param entities Lista de CountryEntity
     * @return Lista de Country
     */
    List<Country> toDomainList(List<CountryEntity> entities);

    /**
     * Convierte una lista de modelos de dominio a lista de entidades de persistencia.
     *
     * @param domains Lista de Country
     * @return Lista de CountryEntity
     */
    List<CountryEntity> toEntityList(List<Country> domains);
}
