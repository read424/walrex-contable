package org.walrex.infrastructure.adapter.outbound.persistence.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.walrex.application.dto.query.DistrictFilter;
import org.walrex.application.dto.query.PageRequest;
import org.walrex.application.dto.query.ProvinceFilter;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.DistrictEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class DistrictRepository implements PanacheRepositoryBase<DistrictEntity, Integer> {
    /**
     * Buscar distrito por id (activos solamente).
     */
    public Uni<DistrictEntity> findActiveById(Integer id){
        return find("id = ?1 and status=true", id).firstResult();
    }

    /**
     * Busca distrito por código ubigeo (activas solamente).
     */
    public Uni<DistrictEntity> findActiveByCode(String code) {
        return find("UPPER(codigo) = ?1 and status=true", code.toUpperCase())
                .firstResult();
    }

    /**
     * Busca distrito por id.
     */
    public Uni<DistrictEntity> findById(Integer id) {
        return find("id = ?1", id)
                .firstResult();
    }

    /**
     * Busca distrito por codigo.
     */
    public Uni<DistrictEntity> findByCode(String code) {
        return find("UPPER(codigo) = ?1", code)
                .firstResult();
    }

    /**
     * Busca distrito por nombre exacto (activas solamente).
     */
    public Uni<DistrictEntity> findByName(String name) {
        return find("lower(name) = ?1 and status=true", name.toLowerCase().trim())
                .firstResult();
    }

    /**
     * Busca distrito por provincia (activas solamente).
     */
    public Uni<DistrictEntity> findByidProvince(Integer idProvince) {
        return find("province.id = ?1 and status=true", idProvince)
                .firstResult();
    }

    // ==================== Verificaciones de Unicidad ====================
    /**
     * Verifica si existe un distrito con el código.
     * Opcionalmente excluye un ID (útil para updates).
     */
    public Uni<Boolean> existsByCode(String code, Integer excludeId) {
        if (excludeId == null) {
            return count("codigo = ?1 and status = true", code.toUpperCase())
                    .map(count -> count > 0);
        }
        return count("codigo = ?1 and status = true and id != ?2",
                code.toUpperCase(), excludeId)
                .map(count -> count > 0);
    }

    /**
     * Verifica si existe un distrito con el nombre.
     */
    public Uni<Boolean> existsByName(String name, Integer excludeId) {
        String normalizedName = name.toLowerCase().trim();
        if (excludeId == null) {
            return count("lower(name) = ?1 and status=true", normalizedName)
                    .map(count -> count > 0);
        }
        return count("lower(name) = ?1 and status=true and id != ?2",
                normalizedName, excludeId)
                .map(count -> count > 0);
    }

    // ==================== Listados con Paginación y Filtros ====================

    /**
     * Lista distritos con paginación, ordenamiento y filtros.
     *
     * Usa objetos de la capa de aplicación (PageRequest y DistrictFilter)
     * siguiendo el patrón hexagonal.
     *
     * @param pageRequest Configuración de paginación y ordenamiento
     * @param filter      Filtros de búsqueda
     * @return Multi que emite los distritos de la página solicitada
     */
    public Multi<DistrictEntity> findWithFilters(PageRequest pageRequest, DistrictFilter filter) {
        StringBuilder query = new StringBuilder("1=1");
        Map<String, Object> params = new HashMap<>();

        // Aplicar filtros del objeto CurrencyFilter
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
    public Uni<Long> countWithFilters(DistrictFilter filter) {
        StringBuilder query = new StringBuilder("1=1");
        Map<String, Object> params = new HashMap<>();

        // Aplicar los mismos filtros que en findWithFilters
        applyFilters(query, params, filter);

        return count(query.toString(), params);
    }

    // ==================== Streaming ====================

    /**
     * Obtiene todos los distritos activas como stream.
     *
     * @return Multi que emite cada departamento activo
     */
    public Multi<DistrictEntity> streamAll() {
        return list("status=true")
                .onItem()
                .transformToMulti(list -> Multi.createFrom().iterable(list));
    }

    // ==================== Operaciones de Soft Delete ====================

    /**
     * Lista todas los distritos eliminados (para posible restauración).
     *
     * @return Uni con lista de distritos eliminados
     */
    public Uni<List<DistrictEntity>> findAllDeleted() {
        return list("status=true");
    }

    // ==================== Métodos Auxiliares ====================

    /**
     * Aplica los filtros del DistrictFilter a la consulta.
     *
     * Este método privado centraliza la lógica de filtrado para evitar duplicación
     * entre findWithFilters y countWithFilters.
     *
     * @param query  StringBuilder con la query base
     * @param params Map de parámetros nombrados
     * @param filter Objeto con los filtros a aplicar
     */
    private void applyFilters(StringBuilder query, Map<String, Object> params, DistrictFilter filter) {
        if (filter == null) {
            // Si no hay filtro, solo aplicar el filtro de soft delete por defecto
            query.append(" and status = true");
            return;
        }

        // Busqueda general por id provincia
        if(filter.getIdProvince()!=null){
            query.append(" and province.id = :idProvince");
            params.put("idProvince", filter.getIdProvince());
        }

        // Búsqueda general (en nombre o código alfabético)
        if (filter.getName() != null && !filter.getName().isBlank()) {
            query.append(" and (lower(name) like :search");
            params.put("search", "%" + filter.getName().toLowerCase().trim() + "%");
        }

        // Filtro exacto por código alfabético
        if (filter.getCodigo() != null && !filter.getCodigo().isBlank()) {
            query.append(" and codigo = :alphabeticCode");
            params.put("alphabeticCode", filter.getCodigo());
        }

        // Filtro por estado (activo/inactivo)
        if (filter.getIncludeDeleted() != null && !filter.getIncludeDeleted()) {
            query.append(" and status = true ");
        }
    }
}
