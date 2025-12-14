package org.walrex.infrastructure.adapter.outbound.persistence.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.walrex.application.dto.query.CountryFilter;
import org.walrex.application.dto.query.PageRequest;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.CountryEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class CountryRepository implements PanacheRepositoryBase<CountryEntity, Integer> {

    public Uni<CountryEntity> findActiveById(Integer id){
        return find("id = ?1 and status='1'", id).firstResult();
    }

    /**
     * Busca moneda por código alfabético (activas solamente).
     */
    public Uni<CountryEntity> findByAlphabeticCode(String code) {
        return find("alphabeticCode3 = ?1 and deletedAt is null", code.toUpperCase())
                .firstResult();
    }

    /**
     * Busca moneda por código numérico (activas solamente).
     */
    public Uni<CountryEntity> findByNumericCode(String code) {
        return find("numericCode = ?1 and deletedAt is null", code)
                .firstResult();
    }

    /**
     * Busca moneda por nombre exacto (activas solamente).
     */
    public Uni<CountryEntity> findByName(String name) {
        return find("lower(name) = ?1 and deletedAt is null", name.toLowerCase().trim())
                .firstResult();
    }

    // ==================== Verificaciones de Unicidad ====================

    /**
     * Verifica si existe una moneda con el código alfabético.
     * Opcionalmente excluye un ID (útil para updates).
     */
    public Uni<Boolean> existsByAlphabeticCode(String code, Integer excludeId) {
        if (excludeId == null) {
            return count("alphabeticCode3 = ?1 and deletedAt is null", code.toUpperCase())
                    .map(count -> count > 0);
        }
        return count("alphabeticCode3 = ?1 and deletedAt is null and id != ?2",
                code.toUpperCase(), excludeId)
                .map(count -> count > 0);
    }

    /**
     * Verifica si existe una moneda con el código numérico.
     */
    public Uni<Boolean> existsByNumericCode(Integer code, Integer excludeId) {
        if (excludeId == null) {
            return count("numericCode = ?1 and deletedAt is null", code)
                    .map(count -> count > 0);
        }
        return count("numericCode = ?1 and deletedAt is null and id != ?2",
                code, excludeId)
                .map(count -> count > 0);
    }

    /**
     * Verifica si existe una moneda con el nombre.
     */
    public Uni<Boolean> existsByName(String name, Integer excludeId) {
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
     * Lista paises con paginación, ordenamiento y filtros.
     *
     * Usa objetos de la capa de aplicación (PageRequest y CountryFilter)
     * siguiendo el patrón hexagonal.
     *
     * @param pageRequest Configuración de paginación y ordenamiento
     * @param filter      Filtros de búsqueda
     * @return Multi que emite los paises de la página solicitada
     */
    public Multi<CountryEntity> findWithFilters(PageRequest pageRequest, CountryFilter filter) {
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
    public Uni<Long> countWithFilters(CountryFilter filter) {
        StringBuilder query = new StringBuilder("1=1");
        Map<String, Object> params = new HashMap<>();

        // Aplicar los mismos filtros que en findWithFilters
        applyFilters(query, params, filter);

        return count(query.toString(), params);
    }

    // ==================== Streaming ====================

    /**
     * Obtiene todos los paises activas como stream.
     *
     * @return Multi que emite cada pais activo
     */
    public Multi<CountryEntity> streamAll() {
        return list("deletedAt is null")
                .onItem()
                .transformToMulti(list -> Multi.createFrom().iterable(list));
    }

    // ==================== Operaciones de Soft Delete ====================

    /**
     * Lista todas los paises eliminadas (para posible restauración).
     *
     * @return Uni con lista de paises eliminadas
     */
    public Uni<List<CountryEntity>> findAllDeleted() {
        return list("deletedAt is not null");
    }

    // ==================== Métodos Auxiliares ====================

    /**
     * Aplica los filtros del CurrencyFilter a la consulta.
     *
     * Este método privado centraliza la lógica de filtrado para evitar duplicación
     * entre findWithFilters y countWithFilters.
     *
     * @param query  StringBuilder con la query base
     * @param params Map de parámetros nombrados
     * @param filter Objeto con los filtros a aplicar
     */
    private void applyFilters(StringBuilder query, Map<String, Object> params, CountryFilter filter) {
        if (filter == null) {
            // Si no hay filtro, solo aplicar el filtro de soft delete por defecto
            query.append(" and deletedAt is null");
            return;
        }

        // Filtro de soft delete
        if ("1".equals(filter.getIncludeDeleted())) {
            query.append(" and deletedAt is null");
        }

        // Búsqueda general (en nombre o código alfabético)
        if (filter.getSearch() != null && !filter.getSearch().isBlank()) {
            query.append(" and (lower(name) like :search or lower(alphabeticCode3) like :search)");
            params.put("search", "%" + filter.getSearch().toLowerCase() + "%");
        }

        // Filtro exacto por código alfabético
        if (filter.getAlphabeticCode3() != null && !filter.getAlphabeticCode3().isBlank()) {
            query.append(" and alphabeticCode3 = :alphabeticCode");
            params.put("alphabeticCode", filter.getAlphabeticCode3().toUpperCase());
        }

        // Filtro exacto por código numérico
        if (filter.getNumericCode() != null) {
            query.append(" and numericCode = :numericCode");
            params.put("numericCode", filter.getNumericCode());
        }

        // Filtro por estado (activo/inactivo)
        if (filter.getIncludeDeleted() != null) {
            query.append(" or deletedAt is null");
        }
    }
}
