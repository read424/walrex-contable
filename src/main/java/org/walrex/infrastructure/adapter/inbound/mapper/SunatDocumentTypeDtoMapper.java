package org.walrex.infrastructure.adapter.inbound.mapper;

import org.mapstruct.*;
import org.walrex.application.dto.response.SunatDocumentTypeResponse;
import org.walrex.domain.model.SunatDocumentType;

import java.util.List;

/**
 * Mapper entre el modelo de dominio SunatDocumentType y los DTOs de respuesta.
 *
 * Usa MapStruct para generar automáticamente el código de mapeo en tiempo de compilación.
 *
 * Responsabilidades:
 * - Convertir SunatDocumentType (dominio) → SunatDocumentTypeResponse (DTO salida)
 * - Exponer solo los campos necesarios al cliente
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.CDI,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR
)
public interface SunatDocumentTypeDtoMapper {
    /**
     * Convierte el modelo de dominio a DTO de respuesta.
     *
     * Mapeos automáticos (todos los campos tienen el mismo nombre):
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
     * @param documentType Modelo de dominio
     * @return SunatDocumentTypeResponse DTO de respuesta
     */
    SunatDocumentTypeResponse toResponse(SunatDocumentType documentType);

    /**
     * Convierte una lista de SunatDocumentType a lista de SunatDocumentTypeResponse.
     *
     * @param documentTypes Lista de modelos de dominio
     * @return Lista de DTOs de respuesta
     */
    List<SunatDocumentTypeResponse> toResponseList(List<SunatDocumentType> documentTypes);
}
