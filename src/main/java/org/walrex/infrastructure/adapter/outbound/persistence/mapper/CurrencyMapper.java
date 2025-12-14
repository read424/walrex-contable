package org.walrex.infrastructure.adapter.outbound.persistence.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.walrex.domain.model.Currency;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.CurrencyEntity;

import java.util.List;

/**
 * Mapper entre el modelo de dominio Currency y la entidad de persistencia CurrencyEntity.
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
public interface CurrencyMapper {

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
     * @param domain Modelo de dominio Currency
     * @return CurrencyEntity para persistencia
     */
    CurrencyEntity toEntity(Currency domain);

    /**
     * Convierte una entidad de persistencia a un modelo de dominio.
     *
     * MapStruct mapea automáticamente todos los campos porque tienen el mismo nombre.
     *
     * @param entity CurrencyEntity de persistencia
     * @return Currency modelo de dominio
     */
    Currency toDomain(CurrencyEntity entity);

    /**
     * Convierte una lista de entidades de persistencia a lista de modelos de dominio.
     *
     * @param entities Lista de CurrencyEntity
     * @return Lista de Currency
     */
    List<Currency> toDomainList(List<CurrencyEntity> entities);

    /**
     * Convierte una lista de modelos de dominio a lista de entidades de persistencia.
     *
     * @param domains Lista de Currency
     * @return Lista de CurrencyEntity
     */
    List<CurrencyEntity> toEntityList(List<Currency> domains);
}