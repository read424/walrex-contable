package org.walrex.infrastructure.adapter.inbound.mapper;

import org.mapstruct.*;
import org.walrex.application.dto.request.CreateSunatDocumentTypeRequest;
import org.walrex.application.dto.request.UpdateSunatDocumentTypeRequest;
import org.walrex.domain.model.SunatDocumentType;

/**
 * Mapper entre DTOs de request y el modelo de dominio SunatDocumentType.
 *
 * Usa MapStruct para generar automáticamente el código de mapeo en tiempo de compilación.
 *
 * Responsabilidades:
 * - Convertir CreateSunatDocumentTypeRequest → SunatDocumentType
 * - Convertir UpdateSunatDocumentTypeRequest → SunatDocumentType
 * - Establecer valores por defecto para nuevos registros
 * - Normalizar datos de entrada
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.CDI,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR
)
public interface SunatDocumentTypeRequestMapper {
    /**
     * Convierte un DTO de creación a modelo de dominio.
     *
     * Mapeos especiales:
     * - active: Se establece como true por defecto
     * - createdAt, updatedAt: Se establecen con timestamp actual
     * - sunatUpdatedAt: Se ignora (se establecerá cuando se sincronice con SUNAT)
     *
     * @param request DTO de creación
     * @return SunatDocumentType modelo de dominio
     */
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "createdAt", expression = "java(java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC))")
    @Mapping(target = "updatedAt", expression = "java(java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC))")
    @Mapping(target = "sunatUpdatedAt", ignore = true)
    SunatDocumentType toModel(CreateSunatDocumentTypeRequest request);

    /**
     * Convierte un DTO de actualización a modelo de dominio.
     *
     * Mapeos especiales:
     * - id: Se ignora (se establecerá desde el path param)
     * - updatedAt: Se establece con timestamp actual
     * - createdAt, sunatUpdatedAt: Se ignoran (no se modifican en updates)
     *
     * @param request DTO de actualización
     * @return SunatDocumentType modelo de dominio
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "updatedAt", expression = "java(java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC))")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "sunatUpdatedAt", ignore = true)
    SunatDocumentType toModel(UpdateSunatDocumentTypeRequest request);
}