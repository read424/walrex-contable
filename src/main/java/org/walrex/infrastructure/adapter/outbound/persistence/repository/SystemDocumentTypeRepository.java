package org.walrex.infrastructure.adapter.outbound.persistence.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.walrex.application.dto.query.PageRequest;
import org.walrex.application.dto.query.SystemDocumentTypeFilter;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.SystemDocumentTypeEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class SystemDocumentTypeRepository implements PanacheRepositoryBase<SystemDocumentTypeEntity, Long> {

    /**
     * Busca tipo de documento por ID (solo activos).
     */
    public Uni<SystemDocumentTypeEntity> findActiveById(Long id) {
        return find("id = ?1 and deletedAt is null", id).firstResult();
    }

    /**
     * Busca tipo de documento por código (activos solamente).
     */
    public Uni<SystemDocumentTypeEntity> findByCode(String code) {
        return find("upper(code) = ?1 and deletedAt is null", code.toUpperCase())
                .firstResult();
    }

    /**
     * Busca tipo de documento por nombre exacto (activos solamente).
     */
    public Uni<SystemDocumentTypeEntity> findByName(String name) {
        return find("lower(name) = ?1 and deletedAt is null", name.toLowerCase().trim())
                .firstResult();
    }

    // ==================== Verificaciones de Unicidad ====================

    /**
     * Verifica si existe un tipo de documento con el código.
     * Opcionalmente excluye un ID (útil para updates).
     */
    public Uni<Boolean> existsByCode(String code, Long excludeId) {
        if (excludeId == null) {
            return count("upper(code) = ?1 and deletedAt is null", code.toUpperCase())
                    .map(count -> count > 0);
        }
        return count("upper(code) = ?1 and deletedAt is null and id != ?2",
                code.toUpperCase(), excludeId)
                .map(count -> count > 0);
    }

    /**
     * Verifica si existe un tipo de documento con el nombre.
     */
    public Uni<Boolean> existsByName(String name, Long excludeId) {
        String normalizedName = name.toLowerCase().trim();
        if (excludeId == null) {
            return count("lower(name) = ?1 and deletedAt is null", normalizedName)
                    .map(count -> count > 0);
        }
        return count("lower(name) = ?1 and deletedAt is null and id != ?2",
                normalizedName, excludeId)
                .map(count -> count > 0);
    }

    // ==================== Listados con Paginación y Filtros ====================

    /**
     * Lista tipos de documento con paginación, ordenamiento y filtros.
     */
    public Multi<SystemDocumentTypeEntity> findWithFilters(PageRequest pageRequest, SystemDocumentTypeFilter filter) {
        StringBuilder query = new StringBuilder("1=1");
        Map<String, Object> params = new HashMap<>();

        // Aplicar filtros del objeto SystemDocumentTypeFilter
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
     */
    public Uni<Long> countWithFilters(SystemDocumentTypeFilter filter) {
        StringBuilder query = new StringBuilder("1=1");
        Map<String, Object> params = new HashMap<>();

        // Aplicar los mismos filtros que en findWithFilters
        applyFilters(query, params, filter);

        return count(query.toString(), params);
    }

    // ==================== Streaming ====================

    /**
     * Obtiene todos los tipos de documento activos como stream.
     */
    public Multi<SystemDocumentTypeEntity> streamAll() {
        return list("deletedAt is null order by priority asc, name asc")
                .onItem()
                .transformToMulti(list -> Multi.createFrom().iterable(list));
    }

    // ==================== Operaciones de Soft Delete ====================

    /**
     * Lista todos los tipos de documento eliminados (para posible restauración).
     */
    public Uni<List<SystemDocumentTypeEntity>> findAllDeleted() {
        return list("deletedAt is not null");
    }

    // ==================== Métodos Auxiliares ====================

    /**
     * Aplica los filtros del SystemDocumentTypeFilter a la consulta.
     */
    private void applyFilters(StringBuilder query, Map<String, Object> params, SystemDocumentTypeFilter filter) {
        if (filter == null) {
            // Si no hay filtro, solo aplicar el filtro de soft delete por defecto
            query.append(" and deletedAt is null");
            return;
        }

        // Filtro de soft delete
        if (!"1".equals(filter.getIncludeDeleted())) {
            query.append(" and deletedAt is null");
        }

        // Búsqueda general (en código, nombre y descripción)
        if (filter.getSearch() != null && !filter.getSearch().isBlank()) {
            query.append(
                    " and (lower(code) like :search or lower(name) like :search or lower(description) like :search)");
            params.put("search", "%" + filter.getSearch().toLowerCase() + "%");
        }

        // Filtro exacto por código
        if (filter.getCode() != null && !filter.getCode().isBlank()) {
            query.append(" and upper(code) = :code");
            params.put("code", filter.getCode().toUpperCase());
        }

        // Filtro por si es requerido
        if (filter.getIsRequired() != null) {
            query.append(" and isRequired = :isRequired");
            params.put("isRequired", filter.getIsRequired());
        }

        // Filtro por si aplica para personas
        if (filter.getForPerson() != null) {
            query.append(" and forPerson = :forPerson");
            params.put("forPerson", filter.getForPerson());
        }

        // Filtro por si aplica para empresas
        if (filter.getForCompany() != null) {
            query.append(" and forCompany = :forCompany");
            params.put("forCompany", filter.getForCompany());
        }

        // Filtro por estado activo
        if (filter.getActive() != null) {
            query.append(" and active = :active");
            params.put("active", filter.getActive());
        }
    }
}
