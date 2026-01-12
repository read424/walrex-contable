package org.walrex.infrastructure.adapter.outbound.persistence.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.walrex.application.dto.query.PageRequest;
import org.walrex.application.dto.query.ProductCategoryUomFilter;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.ProductCategoryUomEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repositorio para operaciones de persistencia de ProductCategoryUom.
 *
 * Usa el patrón Repository de Panache que proporciona:
 * - Operaciones CRUD básicas
 * - Queries tipadas
 * - Paginación integrada
 */
@ApplicationScoped
public class ProductCategoryUomRepository implements PanacheRepositoryBase<ProductCategoryUomEntity, Integer> {

    /**
     * Busca categoría activa (no eliminada) por ID.
     */
    public Uni<ProductCategoryUomEntity> findActiveById(Integer id) {
        return find("id = ?1 and deletedAt is null", id).firstResult();
    }

    /**
     * Busca categoría por código (activas solamente).
     */
    public Uni<ProductCategoryUomEntity> findByCode(String code) {
        return find("code = ?1 and deletedAt is null", code.trim())
                .firstResult();
    }

    /**
     * Busca categoría por nombre exacto (activas solamente).
     */
    public Uni<ProductCategoryUomEntity> findByName(String name) {
        return find("lower(name) = ?1 and deletedAt is null", name.toLowerCase().trim())
                .firstResult();
    }

    // ==================== Verificaciones de Unicidad ====================

    /**
     * Verifica si existe una categoría con el código.
     * Opcionalmente excluye un ID (útil para updates).
     */
    public Uni<Boolean> existsByCode(String code, Integer excludeId) {
        String normalizedCode = code.trim();
        if (excludeId == null) {
            return count("code = ?1 and deletedAt is null", normalizedCode)
                    .map(count -> count > 0);
        }
        return count("code = ?1 and deletedAt is null and id != ?2",
                normalizedCode, excludeId)
                .map(count -> count > 0);
    }

    /**
     * Verifica si existe una categoría con el nombre.
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
     * Lista categorías con paginación, ordenamiento y filtros.
     */
    public Multi<ProductCategoryUomEntity> findWithFilters(PageRequest pageRequest, ProductCategoryUomFilter filter) {
        StringBuilder query = new StringBuilder("1=1");
        Map<String, Object> params = new HashMap<>();

        // Aplicar filtros del objeto ProductCategoryUomFilter
        applyFilters(query, params, filter);

        // Construir Sort
        Sort sort = buildSort(pageRequest);

        // Construir Page
        Page page = Page.of(pageRequest.getPage(), pageRequest.getSize());

        // Ejecutar query con paginación
        return find(query.toString(), sort, params)
                .page(page)
                .list()
                .onItem()
                .transformToMulti(list -> Multi.createFrom().iterable(list));
    }

    /**
     * Lista todas las categorías con filtros (sin paginación).
     */
    public Multi<ProductCategoryUomEntity> findAllWithFilters(ProductCategoryUomFilter filter) {
        StringBuilder query = new StringBuilder("1=1");
        Map<String, Object> params = new HashMap<>();

        // Aplicar filtros del objeto ProductCategoryUomFilter
        applyFilters(query, params, filter);

        // Construir Sort
        Sort sort = Sort.by("code", Sort.Direction.Ascending);

        return find(query.toString(), sort, params)
                .list()
                .onItem()
                .transformToMulti(list -> Multi.createFrom().iterable(list));
    }

    /**
     * Cuenta el total de categorías que cumplen los filtros.
     */
    public Uni<Long> countWithFilters(ProductCategoryUomFilter filter) {
        StringBuilder query = new StringBuilder("1=1");
        Map<String, Object> params = new HashMap<>();

        applyFilters(query, params, filter);

        return count(query.toString(), params);
    }

    /**
     * Stream de todas las categorías activas.
     */
    public Multi<ProductCategoryUomEntity> streamAll() {
        return list("deletedAt is null", Sort.by("name").ascending())
                .onItem()
                .transformToMulti(list -> Multi.createFrom().iterable(list));
    }

    /**
     * Lista todas las categorías eliminadas.
     */
    public Uni<List<ProductCategoryUomEntity>> findAllDeleted() {
        return find("deletedAt is not null", Sort.by("deletedAt").descending())
                .list();
    }

    // ==================== Métodos Privados de Utilidad ====================

    /**
     * Aplica los filtros de ProductCategoryUomFilter a la query.
     */
    private void applyFilters(StringBuilder query, Map<String, Object> params, ProductCategoryUomFilter filter) {
        if (filter == null) {
            query.append(" and deletedAt is null");
            return;
        }

        // Filtro: incluir/excluir eliminados
        if ("1".equals(filter.getIncludeDeleted())) {
            // Incluir eliminados: no agregar condición de deletedAt
        } else {
            query.append(" and deletedAt is null");
        }

        // Filtro: búsqueda general en código o nombre
        if (filter.getSearch() != null && !filter.getSearch().isBlank()) {
            query.append(" and (lower(code) like :search or lower(name) like :search)");
            params.put("search", "%" + filter.getSearch().toLowerCase().trim() + "%");
        }

        // Filtro: código exacto
        if (filter.getCode() != null && !filter.getCode().isBlank()) {
            query.append(" and code = :code");
            params.put("code", filter.getCode().trim());
        }

        // Filtro: nombre exacto
        if (filter.getName() != null && !filter.getName().isBlank()) {
            query.append(" and lower(name) = :name");
            params.put("name", filter.getName().toLowerCase().trim());
        }

        // Filtro: estado activo/inactivo
        if (filter.getActive() != null && !filter.getActive().isBlank()) {
            boolean isActive = "1".equals(filter.getActive());
            query.append(" and active = :active");
            params.put("active", isActive);
        }
    }

    /**
     * Construye el objeto Sort basado en PageRequest.
     */
    private Sort buildSort(PageRequest pageRequest) {
        String sortField = validateAndNormalizeSortField(pageRequest.getSortBy());

        return pageRequest.getSortDirection() == PageRequest.SortDirection.DESCENDING
                ? Sort.by(sortField).descending()
                : Sort.by(sortField).ascending();
    }

    /**
     * Valida y normaliza el campo de ordenamiento para prevenir SQL injection.
     */
    private String validateAndNormalizeSortField(String field) {
        if (field == null || field.isBlank()) {
            return "name"; // Default
        }

        return switch (field.toLowerCase().trim()) {
            case "id" -> "id";
            case "code" -> "code";
            case "name" -> "name";
            case "description" -> "description";
            case "active", "is_active" -> "active";
            case "createdat", "created_at" -> "createdAt";
            case "updatedat", "updated_at" -> "updatedAt";
            default -> "name"; // Default seguro
        };
    }
}
