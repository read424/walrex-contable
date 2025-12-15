package org.walrex.application.port.output;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.walrex.application.dto.query.PageRequest;
import org.walrex.application.dto.query.SunatDocumentTypeFilter;
import org.walrex.domain.model.PagedResult;
import org.walrex.domain.model.SunatDocumentType;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida para operaciones de lectura en tipos de documentos SUNAT.
 *
 * Define el contrato para consultas (CQRS - Query side).
 * La implementación se encuentra en la capa de infraestructura (Adapter).
 */
public interface SunatDocumentTypeQueryPort {

    // ==================== Búsquedas Básicas ====================

    /**
     * Busca un tipo de documento por su ID.
     *
     * @param id Identificador único
     * @return Uni con Optional del tipo de documento (solo activos)
     */
    Uni<Optional<SunatDocumentType>> findById(Integer id);

    /**
     * Busca un tipo de documento por su ID (incluyendo inactivos).
     *
     * @param id Identificador único
     * @return Uni con Optional del tipo de documento
     */
    Uni<Optional<SunatDocumentType>> findByIdIncludingInactive(Integer id);

    /**
     * Busca un tipo de documento por su código SUNAT.
     *
     * @param code Código SUNAT
     * @return Uni con Optional del tipo de documento (solo activos)
     */
    Uni<Optional<SunatDocumentType>> findByCode(String code);

    // ==================== Verificaciones de Existencia ====================

    /**
     * Verifica si existe un tipo de documento con el ID especificado.
     *
     * @param id ID a verificar
     * @param excludeId ID a excluir de la verificación (útil para updates)
     * @return Uni<Boolean> true si existe, false en caso contrario
     */
    Uni<Boolean> existsById(Integer id, String excludeId);

    /**
     * Verifica si existe un tipo de documento con el código especificado.
     *
     * @param code Código a verificar
     * @param excludeId ID a excluir de la verificación
     * @return Uni<Boolean> true si existe, false en caso contrario
     */
    Uni<Boolean> existsByCode(String code, String excludeId);

    // ==================== Listados con Paginación ====================

    /**
     * Lista tipos de documentos con paginación, ordenamiento y filtros.
     *
     * @param pageRequest Configuración de paginación y ordenamiento
     * @param filter Filtros de búsqueda
     * @return Uni con PagedResult que contiene los tipos de documentos y metadatos de paginación
     */
    Uni<PagedResult<SunatDocumentType>> findAll(PageRequest pageRequest, SunatDocumentTypeFilter filter);

    /**
     * Cuenta el total de tipos de documentos con los filtros aplicados.
     *
     * @param filter Filtros de búsqueda
     * @return Uni con el conteo total
     */
    Uni<Long> count(SunatDocumentTypeFilter filter);

    // ==================== Streaming ====================

    /**
     * Obtiene todos los tipos de documentos activos como stream.
     *
     * @return Multi que emite cada tipo de documento activo
     */
    Multi<SunatDocumentType> streamAll();

    /**
     * Obtiene tipos de documentos como stream con filtros aplicados.
     *
     * @param filter Filtros a aplicar
     * @return Multi que emite cada tipo de documento que cumple los filtros
     */
    Multi<SunatDocumentType> streamWithFilter(SunatDocumentTypeFilter filter);

    // ==================== Consultas Especiales ====================

    /**
     * Lista todos los tipos de documentos inactivos.
     *
     * @return Uni con lista de tipos de documentos inactivos
     */
    Uni<List<SunatDocumentType>> findAllInactive();

    /**
     * Busca tipos de documentos por una longitud específica.
     *
     * @param length Longitud del documento
     * @return Uni con lista de tipos de documentos que tienen esa longitud
     */
    Uni<List<SunatDocumentType>> findByLength(Integer length);
}
