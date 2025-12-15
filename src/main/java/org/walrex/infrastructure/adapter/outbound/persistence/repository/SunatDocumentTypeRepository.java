package org.walrex.infrastructure.adapter.outbound.persistence.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.walrex.application.dto.query.PageRequest;
import org.walrex.application.dto.query.SunatDocumentTypeFilter;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.SunatDocumentTypeEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repositorio de Panache para tipos de documentos SUNAT.
 *
 * Extiende PanacheRepositoryBase con String como tipo de ID (no Integer).
 * Proporciona métodos de consulta personalizados siguiendo los patrones de Country.
 */
@ApplicationScoped
public class SunatDocumentTypeRepository implements PanacheRepositoryBase<SunatDocumentTypeEntity, Integer> {

    /**
     * Busca un tipo de documento activo por su ID.
     *
     * @param id Identificador del tipo de documento
     * @return Uni con la entidad si existe y está activa, null en caso contrario
     */
    public Uni<SunatDocumentTypeEntity> findActiveById(Integer id) {
        return find("id = ?1 and active = true", id).firstResult();
    }

    /**
     * Busca un tipo de documento por código SUNAT (solo activos).
     *
     * @param code Código SUNAT
     * @return Uni con la entidad si existe y está activa
     */
    public Uni<SunatDocumentTypeEntity> findByCode(String code) {
        return find("code = ?1 and active = true", code.toUpperCase())
                .firstResult();
    }

    // ==================== Verificaciones de Unicidad ====================

    /**
     * Verifica si existe un tipo de documento con el ID especificado.
     * Opcionalmente excluye un ID (útil para updates).
     *
     * @param id ID a verificar
     * @param excludeId ID a excluir de la verificación
     * @return Uni<Boolean> true si existe
     */
    public Uni<Boolean> existsById(Integer id, String excludeId) {
        if (excludeId == null) {
            return count("id = ?1 and active = true", id)
                    .map(count -> count > 0);
        }
        return count("id = ?1 and active = true and id != ?2",
                id, excludeId)
                .map(count -> count > 0);
    }

    /**
     * Verifica si existe un tipo de documento con el código especificado.
     *
     * @param code Código a verificar
     * @param excludeId ID a excluir de la verificación
     * @return Uni<Boolean> true si existe
     */
    public Uni<Boolean> existsByCode(String code, String excludeId) {
        if (excludeId == null) {
            return count("code = ?1 and active = true", code.toUpperCase())
                    .map(count -> count > 0);
        }
        return count("code = ?1 and active = true and id != ?2",
                code.toUpperCase(), excludeId)
                .map(count -> count > 0);
    }

    // ==================== Listados con Paginación y Filtros ====================

    /**
     * Lista tipos de documentos con paginación, ordenamiento y filtros.
     *
     * @param pageRequest Configuración de paginación y ordenamiento
     * @param filter Filtros de búsqueda
     * @return Multi que emite los tipos de documentos de la página solicitada
     */
    public Multi<SunatDocumentTypeEntity> findWithFilters(PageRequest pageRequest, SunatDocumentTypeFilter filter) {
        StringBuilder query = new StringBuilder("1=1");
        Map<String, Object> params = new HashMap<>();

        // Aplicar filtros del objeto SunatDocumentTypeFilter
        applyFilters(query, params, filter);

        // Ordenamiento
        Sort sort = Sort.by(pageRequest.getSortBy());
        if (pageRequest.getSortDirection() == PageRequest.SortDirection.DESCENDING) {
            sort = sort.descending();
        }

        // Aplicar paginación y convertir a Multi
        return find(query.toString(), sort, params)
                .page(Page.of(pageRequest.getPage(), pageRequest.getSize()))
                .list()
                .onItem()
                .transformToMulti(list -> Multi.createFrom().iterable(list));
    }

    /**
     * Cuenta el total de registros con los mismos filtros (para paginación).
     *
     * @param filter Filtros de búsqueda
     * @return Uni con el conteo total
     */
    public Uni<Long> countWithFilters(SunatDocumentTypeFilter filter) {
        StringBuilder query = new StringBuilder("1=1");
        Map<String, Object> params = new HashMap<>();

        // Aplicar los mismos filtros que en findWithFilters
        applyFilters(query, params, filter);

        return count(query.toString(), params);
    }

    // ==================== Streaming ====================

    /**
     * Obtiene todos los tipos de documentos activos como stream.
     *
     * @return Multi que emite cada tipo de documento activo
     */
    public Multi<SunatDocumentTypeEntity> streamAll() {
        return list("active = true")
                .onItem()
                .transformToMulti(list -> Multi.createFrom().iterable(list));
    }

    // ==================== Operaciones de Activación/Desactivación ====================

    /**
     * Lista todos los tipos de documentos inactivos.
     *
     * @return Uni con lista de tipos de documentos inactivos
     */
    public Uni<List<SunatDocumentTypeEntity>> findAllInactive() {
        return list("active = false");
    }

    /**
     * Lista tipos de documentos por longitud específica.
     *
     * @param length Longitud del documento
     * @return Uni con lista de tipos de documentos
     */
    public Uni<List<SunatDocumentTypeEntity>> findByLength(Integer length) {
        return list("length = ?1 and active = true", length);
    }

    // ==================== Métodos Auxiliares ====================

    /**
     * Aplica los filtros del SunatDocumentTypeFilter a la consulta.
     *
     * Este método privado centraliza la lógica de filtrado para evitar duplicación
     * entre findWithFilters y countWithFilters.
     *
     * @param query StringBuilder con la query base
     * @param params Map de parámetros nombrados
     * @param filter Objeto con los filtros a aplicar
     */
    private void applyFilters(StringBuilder query, Map<String, Object> params, SunatDocumentTypeFilter filter) {
        if (filter == null) {
            // Si no hay filtro, solo aplicar el filtro de activos por defecto
            query.append(" and active = true");
            return;
        }

        // Filtro de activos/inactivos
        if (!filter.shouldIncludeInactive()) {
            query.append(" and active = true");
        }

        // Búsqueda general (en nombre, código o ID)
        if (filter.getSearch() != null && !filter.getSearch().isBlank()) {
            query.append(" and (lower(name) like :search or lower(code) like :search or lower(id) like :search)");
            params.put("search", "%" + filter.getSearch().toLowerCase() + "%");
        }

        // Filtro exacto por código
        if (filter.getCode() != null && !filter.getCode().isBlank()) {
            query.append(" and code = :code");
            params.put("code", filter.getCode().toUpperCase());
        }

        // Filtro por estado activo
        if (filter.getActive() != null) {
            query.append(" and active = :active");
            params.put("active", filter.getActive());
        }

        // Filtro por longitud
        if (filter.getLength() != null) {
            query.append(" and length = :length");
            params.put("length", filter.getLength());
        }
    }
}
