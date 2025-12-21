package org.walrex.infrastructure.adapter.outbound.persistence.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.walrex.application.dto.query.PageRequest;
import org.walrex.application.dto.query.ProvinceFilter;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.DistrictEntity;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.ProvinceEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@ApplicationScoped
public class ProvinceRepository  implements PanacheRepositoryBase<ProvinceEntity, Integer> {

    /**
     * Buscar provincia por id (activos solamente).
     */
    public Uni<ProvinceEntity> findActiveById(Integer id){
        return find("id = ?1 and status=true", id).firstResult();
    }

    /**
     * Busca provincia por código ubigeo (activas solamente).
     */
    public Uni<ProvinceEntity> findActiveByCode(String code) {
        return find("UPPER(codigo) = ?1 and status=true", code.toUpperCase())
                .firstResult();
    }

    /**
     * Busca provincia por id.
     */
    public Uni<ProvinceEntity> findById(Integer id) {
        return find("id = ?1", id)
                .firstResult();
    }

    /**
     * Busca provincia por codigo.
     */
    public Uni<ProvinceEntity> findByCode(String code) {
        return find("UPPER(codigo) = ?1", code)
                .firstResult();
    }

    /**
     * Busca provincia por nombre (activas solamente).
     */
    public Uni<ProvinceEntity> findByName(String name) {
        return find("lower(name) like ?1 and status=true", '%'+name.toLowerCase().trim()+'%')
                .firstResult();
    }

    /**
     * Busca provincia por id departamento (activas solamente).
     */
    public Multi<ProvinceEntity> findByIdDepartamento(Integer idDepartamento) {
        return list("departament.id = ?1 and status=true", idDepartamento)
                .onItem()
                .transformToMulti(list -> Multi.createFrom().iterable(list));
    }

    // ==================== Verificaciones de Unicidad ====================

    /**
     * Verifica si existe un provincia con el código.
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
     * Verifica si existe una provincia con el nombre en un departamento específico.
     */
    public Uni<Boolean> existsByNameForDepartment(String name, Integer idDepartment, Integer excludeId) {
        String normalizedName = name.toLowerCase().trim();
        if (excludeId == null) {
            return count("lower(name) = ?1 and departament.id = ?2 and status=true",
                    normalizedName, idDepartment)
                    .map(count -> count > 0);
        }
        return count("lower(name) = ?1 and departament.id = ?2 and status=true and id != ?3",
                normalizedName, idDepartment, excludeId)
                .map(count -> count > 0);
    }

    // ==================== Listados con Paginación y Filtros ====================

    /**
     * Lista provincia con paginación, ordenamiento y filtros.
     *
     * Usa objetos de la capa de aplicación (PageRequest y ProvinceFilter)
     * siguiendo el patrón hexagonal.
     *
     * @param pageRequest Configuración de paginación y ordenamiento
     * @param filter      Filtros de búsqueda
     * @return Multi que emite los provincia de la página solicitada
     */
    public Multi<ProvinceEntity> findWithFilters(PageRequest pageRequest, ProvinceFilter filter) {
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
    public Uni<Long> countWithFilters(ProvinceFilter filter) {
        StringBuilder query = new StringBuilder("1=1");
        Map<String, Object> params = new HashMap<>();

        // Aplicar los mismos filtros que en findWithFilters
        applyFilters(query, params, filter);

        return count(query.toString(), params);
    }

    // ==================== Streaming ====================

    /**
     * Obtiene todos los provincia activas como stream.
     *
     * @return Multi que emite cada provincia activo
     */
    public Multi<ProvinceEntity> streamAll() {
        return list("status=true")
                .onItem()
                .transformToMulti(list -> Multi.createFrom().iterable(list));
    }

    // ==================== Operaciones de Soft Delete ====================

    /**
     * Lista todas los provincias eliminados (para posible restauración).
     *
     * @return Uni con lista de provincias eliminados
     */
    public Uni<List<ProvinceEntity>> findAllDeleted() {
        return list("status=true");
    }

    // ==================== Métodos Auxiliares ====================

    /**
     * Aplica los filtros del ProvinceFilter a la consulta.
     *
     * Este método privado centraliza la lógica de filtrado para evitar duplicación
     * entre findWithFilters y countWithFilters.
     *
     * @param query  StringBuilder con la query base
     * @param params Map de parámetros nombrados
     * @param filter Objeto con los filtros a aplicar
     */
    private void applyFilters(StringBuilder query, Map<String, Object> params, ProvinceFilter filter) {
        if (filter == null) {
            // Si no hay filtro, solo aplicar el filtro de soft delete por defecto
            query.append(" and status = true");
            return;
        }

        // Busqueda general por id departamento
        if(filter.getIdDepartament()!=null){
            query.append(" and departament.id = :idDepartamento");
            params.put("idDepartamento", filter.getIdDepartament());
        }

        // Búsqueda general (en nombre)
        if (filter.getName() != null && !filter.getName().isBlank()) {
            query.append(" and lower(name) like :search");
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
