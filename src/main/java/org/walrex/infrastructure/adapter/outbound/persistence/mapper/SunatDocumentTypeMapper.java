package org.walrex.infrastructure.adapter.outbound.persistence.mapper;

import org.mapstruct.*;
import org.walrex.domain.model.SunatDocumentType;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.SunatDocumentTypeEntity;

import java.util.List;

/**
 * Mapper entre el modelo de dominio SunatDocumentType y la entidad de persistencia SunatDocumentTypeEntity.
 *
 * Usa MapStruct para generar automáticamente el código de mapeo en tiempo de compilación.
 *
 * Siguiendo el patrón hexagonal, este mapper pertenece a la capa de infraestructura
 * ya que conoce tanto el modelo de dominio como los detalles de persistencia.
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.CDI,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR
)
public interface SunatDocumentTypeMapper {
    /**
     * Convierte una entidad de dominio a una entidad de persistencia.
     *
     * MapStruct mapea automáticamente todos los campos porque tienen el mismo nombre:
     * - id → id
     * - code → code
     * - name → name
     * - description → description
     * - length → length
     * - pattern → pattern
     * - sunatUpdatedAt → sunatUpdatedAt
     * - active → active
     * - createdAt → createdAt
     * - updatedAt → updatedAt
     *
     * @param domain Modelo de dominio SunatDocumentType
     * @return SunatDocumentTypeEntity para persistencia
     */
    SunatDocumentTypeEntity toEntity(SunatDocumentType domain);

    /**
     * Convierte una entidad de persistencia a un modelo de dominio.
     *
     * MapStruct mapea automáticamente todos los campos porque tienen el mismo nombre.
     *
     * @param entity SunatDocumentTypeEntity de persistencia
     * @return SunatDocumentType modelo de dominio
     */
    SunatDocumentType toDomain(SunatDocumentTypeEntity entity);

    /**
     * Convierte una lista de entidades de persistencia a lista de modelos de dominio.
     *
     * @param entities Lista de SunatDocumentTypeEntity
     * @return Lista de SunatDocumentType
     */
    List<SunatDocumentType> toDomainList(List<SunatDocumentTypeEntity> entities);

    /**
     * Convierte una lista de modelos de dominio a lista de entidades de persistencia.
     *
     * @param domains Lista de SunatDocumentType
     * @return Lista de SunatDocumentTypeEntity
     */
    List<SunatDocumentTypeEntity> toEntityList(List<SunatDocumentType> domains);
}
