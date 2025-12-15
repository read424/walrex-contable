package org.walrex.infrastructure.adapter.outbound.persistence.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.walrex.domain.model.SystemDocumentType;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.SystemDocumentTypeEntity;

import java.util.List;

/**
 * Mapper entre la entidad de persistencia y el modelo de dominio.
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.CDI,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR
)
public interface SystemDocumentTypeMapper {

    /**
     * Convierte de entidad JPA a modelo de dominio.
     */
    SystemDocumentType toDomain(SystemDocumentTypeEntity entity);

    /**
     * Convierte de modelo de dominio a entidad JPA.
     */
    @Mapping(target = "deletedAt", ignore = true)
    SystemDocumentTypeEntity toEntity(SystemDocumentType domain);

    /**
     * Convierte lista de entidades a lista de modelos de dominio.
     */
    List<SystemDocumentType> toDomainList(List<SystemDocumentTypeEntity> entities);

    /**
     * Convierte lista de modelos de dominio a lista de entidades.
     */
    List<SystemDocumentTypeEntity> toEntityList(List<SystemDocumentType> domains);
}
