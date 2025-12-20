package org.walrex.infrastructure.adapter.outbound.persistence.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.walrex.application.dto.query.DepartamentFilter;
import org.walrex.application.dto.query.PageRequest;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.DepartamentEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class DepartamentRepository implements PanacheRepositoryBase<DepartamentEntity, Integer> {

    /**
     * Buscar departamento por id (activos solamente).
     */
    public Uni<DepartamentEntity> findActiveById(Integer id) {
        return find("id = ?1 and status=true", id).firstResult();
    }

    /**
     * Busca departamento por código ubigeo (activas solamente).
     */
    public Uni<DepartamentEntity> findActiveByCode(String code) {
        return find("UPPER(codigo) = ?1 and status=true", code.toUpperCase())
                .firstResult();
    }

    /**
     * Busca departamento por id.
     */
    public Uni<DepartamentEntity> findById(Integer id) {
        return find("id = ?1", id)
                .firstResult();
    }

    /**
     * Busca departamento por codigo.
     */
    public Uni<DepartamentEntity> findByCode(String code) {
        return find("UPPER(codigo) = ?1", code)
                .firstResult();
    }

    /**
     * Busca departamento por nombre exacto (activas solamente).
     */
    public Uni<DepartamentEntity> findByName(String name) {
        return find("lower(nombre) = ?1 and status=true", name.toLowerCase().trim())
                .firstResult();
    }

    // ==================== Verificaciones de Unicidad ====================

    /**
     * Verifica si existe un departamento con el código.
     * Opcionalmente excluye un ID (útil para updates).
     */
    public Uni<Boolean> existsByCode(String code, Integer excludeId) {
        if (excludeId == null) {
            return count("codigo = ?1 and status=true", code.toUpperCase())
                    .map(count -> count > 0);
        }
        return count("codigo = ?1 and status=true and id != ?2",
                code.toUpperCase(), excludeId)
                .map(count -> count > 0);
    }

    /**
     * Verifica si existe un departamento con el nombre.
     */
    public Uni<Boolean> existsByName(String name, Integer excludeId) {
        String normalizedName = name.toLowerCase().trim();
        if (excludeId == null) {
            return count("lower(nombre) = ?1 and status=true", normalizedName)
                    .map(count -> count > 0);
        }
        return count("lower(nombre) = ?1 and status=true and id != ?2",
                normalizedName, excludeId)
                .map(count -> count > 0);
    }

    // ==================== Listados con Paginación y Filtros ====================

    /**
     * Lista departamentos con paginación, ordenamiento y filtros.
     *
     * Usa objetos de la capa de aplicación (PageRequest y DepartamentFilter)
     * siguiendo el patrón hexagonal.
     *
     * @param pageRequest Configuración de paginación y ordenamiento
     * @param filter      Filtros de búsqueda
     * @return Multi que emite los departamentos de la página solicitada
     */
    public Multi<DepartamentEntity> findWithFilters(PageRequest pageRequest, DepartamentFilter filter) {
        StringBuilder query = new StringBuilder("1=1");
        Map<String, Object> params = new HashMap<>();

        // Aplicar filtros del objeto DepartamentFilter
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
    public Uni<Long> countWithFilters(DepartamentFilter filter) {
        StringBuilder query = new StringBuilder("1=1");
        Map<String, Object> params = new HashMap<>();

        // Aplicar los mismos filtros que en findWithFilters
        applyFilters(query, params, filter);

        return count(query.toString(), params);
    }

    // ==================== Streaming ====================

    /**
     * Obtiene todos los departamentos activas como stream.
     *
     * @return Multi que emite cada departamento activo
     */
    public Multi<DepartamentEntity> streamAll() {
        return list("status=true")
                .onItem()
                .transformToMulti(list -> Multi.createFrom().iterable(list));
    }

    // ==================== Operaciones de Soft Delete ====================

    /**
     * Lista todas los departamentos eliminados (para posible restauración).
     *
     * @return Uni con lista de departamentos eliminados
     */
    public Uni<List<DepartamentEntity>> findAllDeleted() {
        return list("status=true");
    }

    // ==================== Métodos Auxiliares ====================

    /**
     * Aplica los filtros del DepartamentFilter a la consulta.
     *
     * Este método privado centraliza la lógica de filtrado para evitar duplicación
     * entre findWithFilters y countWithFilters.
     *
     * @param query  StringBuilder con la query base
     * @param params Map de parámetros nombrados
     * @param filter Objeto con los filtros a aplicar
     */
    private void applyFilters(StringBuilder query, Map<String, Object> params, DepartamentFilter filter) {
        if (filter == null) {
            // Si no hay filtro, solo aplicar el filtro de soft delete por defecto
            query.append(" and status = true");
            return;
        }

        // Búsqueda general (en nombre o código)
        // search string para name y code string para codigo
        if (filter.getName() != null && !filter.getName().isBlank()) {
            query.append(" and lower(nombre) like :search");
            params.put("search", "%" + filter.getName().toLowerCase().trim() + "%");
        }

        // Filtro exacto por código
        if (filter.getCodigo() != null && !filter.getCodigo().isBlank()) {
            query.append(" and codigo = :codigo");
            params.put("codigo", filter.getCodigo());
        }

        // Filtro por estado (activo/inactivo)
        if (filter.getIncludeDeleted() != null && !filter.getIncludeDeleted()) {
            query.append(" and status = true ");
        }
    }
}
