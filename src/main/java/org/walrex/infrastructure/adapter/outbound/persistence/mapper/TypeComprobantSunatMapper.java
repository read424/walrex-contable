package org.walrex.infrastructure.adapter.outbound.persistence.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.walrex.domain.model.TypeComprobantSunat;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.TypeComprobantSunatEntity;

import java.util.List;

/**
 * Mapper between the domain model TypeComprobantSunat and the persistence entity TypeComprobantSunatEntity.
 *
 * Uses MapStruct to automatically generate mapping code at compile time.
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.CDI,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR
)
public interface TypeComprobantSunatMapper {

    /**
     * Converts a persistence entity to a domain model.
     *
     * @param entity TypeComprobantSunatEntity from persistence
     * @return TypeComprobantSunat domain model
     */
    TypeComprobantSunat toDomain(TypeComprobantSunatEntity entity);

    /**
     * Converts a list of persistence entities to a list of domain models.
     *
     * @param entities List of TypeComprobantSunatEntity
     * @return List of TypeComprobantSunat
     */
    List<TypeComprobantSunat> toDomainList(List<TypeComprobantSunatEntity> entities);
}
