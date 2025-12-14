package org.walrex.infrastructure.adapter.outbound.persistence.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.walrex.application.dto.query.CurrencyFilter;
import org.walrex.application.dto.query.PageRequest;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.CurrencyEntity;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Repositorio para operaciones de persistencia de Currency.
 *
 * Usa el patrón Repository de Panache que proporciona:
 * - Operaciones CRUD básicas
 * - Queries tipadas
 * - Paginación integrada
 *
 */
@ApplicationScoped
public class CurrencyRepository implements PanacheRepositoryBase<CurrencyEntity, Integer> {

    /**
     * Busca moneda activa (no eliminada) por ID.
     */
    public Uni<CurrencyEntity> findActiveById(Integer id) {
        return find("id = ?1 and deletedAt is null", id).firstResult();
    }

    /**
     * Busca moneda por código alfabético (activas solamente).
     */
    public Uni<CurrencyEntity> findByAlphabeticCode(String code) {
        return find("alphabeticCode = ?1 and deletedAt is null", code.toUpperCase())
                .firstResult();
    }

    /**
     * Busca moneda por código numérico (activas solamente).
     */
    public Uni<CurrencyEntity> findByNumericCode(String code) {
        return find("numericCode = ?1 and deletedAt is null", code)
                .firstResult();
    }

    /**
     * Busca moneda por nombre exacto (activas solamente).
     */
    public Uni<CurrencyEntity> findByName(String name) {
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
            return count("alphabeticCode = ?1 and deletedAt is null", code.toUpperCase())
                    .map(count -> count > 0);
        }
        return count("alphabeticCode = ?1 and deletedAt is null and id != ?2",
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
     * Lista monedas con paginación, ordenamiento y filtros.
     *
     * Usa objetos de la capa de aplicación (PageRequest y CurrencyFilter)
     * siguiendo el patrón hexagonal.
     *
     * @param pageRequest Configuración de paginación y ordenamiento
     * @param filter      Filtros de búsqueda
     * @return Multi que emite las monedas de la página solicitada
     */
    public Multi<CurrencyEntity> findWithFilters(PageRequest pageRequest, CurrencyFilter filter) {
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
    public Uni<Long> countWithFilters(CurrencyFilter filter) {
        StringBuilder query = new StringBuilder("1=1");
        Map<String, Object> params = new HashMap<>();

        // Aplicar los mismos filtros que en findWithFilters
        applyFilters(query, params, filter);

        return count(query.toString(), params);
    }

    // ==================== Streaming ====================

    /**
     * Obtiene todas las monedas activas como stream.
     *
     * @return Multi que emite cada moneda activa
     */
    public Multi<CurrencyEntity> streamAll() {
        return list("deletedAt is null")
                .onItem()
                .transformToMulti(list -> Multi.createFrom().iterable(list));
    }

    // ==================== Operaciones de Soft Delete ====================

    /**
     * Lista todas las monedas eliminadas (para posible restauración).
     *
     * @return Uni con lista de monedas eliminadas
     */
    public Uni<List<CurrencyEntity>> findAllDeleted() {
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
    private void applyFilters(StringBuilder query, Map<String, Object> params, CurrencyFilter filter) {
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
            query.append(" and (lower(name) like :search or lower(alphabeticCode) like :search)");
            params.put("search", "%" + filter.getSearch().toLowerCase() + "%");
        }

        // Filtro exacto por código alfabético
        if (filter.getAlphabeticCode() != null && !filter.getAlphabeticCode().isBlank()) {
            query.append(" and numericCode = :alphabeticCode");
            params.put("alphabeticCode", filter.getAlphabeticCode().toUpperCase());
        }

        // Filtro exacto por código numérico
        if (filter.getNumericCode() != null && !filter.getNumericCode().isBlank()) {
            query.append(" and numericCode = :numericCode");
            params.put("numericCode", filter.getNumericCode());
        }

        // Filtro por estado (activo/inactivo)
        if (filter.getStatus() != null) {
            query.append(" and status = :status");
            params.put("status", filter.getStatus());
        }
    }
}
