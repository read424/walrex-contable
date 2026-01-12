package org.walrex.infrastructure.adapter.outbound.persistence.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.walrex.application.dto.query.PageRequest;
import org.walrex.application.dto.query.ProductUomFilter;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.ProductUomEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repositorio para operaciones de persistencia de ProductUom.
 *
 * Usa el patrón Repository de Panache que proporciona:
 * - Operaciones CRUD básicas
 * - Queries tipadas
 * - Paginación integrada
 * - JOIN FETCH para cargar la categoría relacionada y evitar N+1 queries
 */
@ApplicationScoped
public class ProductUomRepository implements PanacheRepositoryBase<ProductUomEntity, Integer> {

    /**
     * Busca unidad de medida activa (no eliminada) por ID.
     * Incluye JOIN FETCH para cargar la categoría relacionada.
     */
    public Uni<ProductUomEntity> findActiveById(Integer id) {
        return find("SELECT u FROM ProductUomEntity u LEFT JOIN FETCH u.category " +
                "WHERE u.id = ?1 AND u.deletedAt IS NULL", id)
                .firstResult();
    }

    /**
     * Busca unidad de medida por código (activas solamente).
     */
    public Uni<ProductUomEntity> findByCode(String code) {
        return find("codeUom = ?1 and deletedAt is null", code.trim())
                .firstResult();
    }

    /**
     * Busca unidad de medida por nombre exacto (activas solamente).
     */
    public Uni<ProductUomEntity> findByName(String name) {
        return find("lower(nameUom) = ?1 and deletedAt is null", name.toLowerCase().trim())
                .firstResult();
    }

    // ==================== Verificaciones de Unicidad ====================

    /**
     * Verifica si existe una unidad de medida con el código.
     * Opcionalmente excluye un ID (útil para updates).
     */
    public Uni<Boolean> existsByCode(String code, Integer excludeId) {
        String normalizedCode = code.trim();
        if (excludeId == null) {
            return count("codeUom = ?1 and deletedAt is null", normalizedCode)
                    .map(count -> count > 0);
        }
        return count("codeUom = ?1 and deletedAt is null and id != ?2",
                normalizedCode, excludeId)
                .map(count -> count > 0);
    }

    /**
     * Verifica si existe una unidad de medida con el nombre.
     */
    public Uni<Boolean> existsByName(String name, Integer excludeId) {
        String normalizedName = name.toLowerCase().trim();
        if (excludeId == null) {
            return count("lower(nameUom) = ?1 and deletedAt is null", normalizedName)
                    .map(count -> count > 0);
        }
        return count("lower(nameUom) = ?1 and deletedAt is null and id != ?2",
                normalizedName, excludeId)
                .map(count -> count > 0);
    }

    // ==================== Listados con Paginación y Filtros ====================

    /**
     * Lista unidades de medida con paginación, ordenamiento y filtros.
     * IMPORTANTE: Incluye JOIN FETCH para cargar la categoría relacionada.
     */
    public Multi<ProductUomEntity> findWithFilters(PageRequest pageRequest, ProductUomFilter filter) {
        StringBuilder query = new StringBuilder(
                "SELECT u FROM ProductUomEntity u LEFT JOIN FETCH u.category WHERE 1=1");
        Map<String, Object> params = new HashMap<>();

        // Aplicar filtros del objeto ProductUomFilter
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
     * Lista todas las unidades de medida con filtros (sin paginación).
     * IMPORTANTE: Incluye JOIN FETCH para cargar la categoría relacionada.
     */
    public Multi<ProductUomEntity> findAllWithFilters(ProductUomFilter filter) {
        StringBuilder query = new StringBuilder(
                "SELECT u FROM ProductUomEntity u LEFT JOIN FETCH u.category WHERE 1=1");
        Map<String, Object> params = new HashMap<>();

        // Aplicar filtros del objeto ProductUomFilter
        applyFilters(query, params, filter);

        // Construir Sort
        Sort sort = Sort.by("codeUom", Sort.Direction.Ascending);

        return find(query.toString(), sort, params)
                .list()
                .onItem()
                .transformToMulti(list -> Multi.createFrom().iterable(list));
    }

    /**
     * Cuenta el total de unidades de medida que cumplen los filtros.
     */
    public Uni<Long> countWithFilters(ProductUomFilter filter) {
        StringBuilder query = new StringBuilder("1=1");
        Map<String, Object> params = new HashMap<>();

        applyFilters(query, params, filter);

        return count(query.toString(), params);
    }

    /**
     * Stream de todas las unidades de medida activas.
     * Incluye JOIN FETCH para cargar la categoría relacionada.
     */
    public Multi<ProductUomEntity> streamAll() {
        return list("SELECT u FROM ProductUomEntity u LEFT JOIN FETCH u.category " +
                        "WHERE u.deletedAt IS NULL ORDER BY u.nameUom ASC")
                .onItem()
                .transformToMulti(list -> Multi.createFrom().iterable(list));
    }

    /**
     * Lista todas las unidades de medida eliminadas.
     */
    public Uni<List<ProductUomEntity>> findAllDeleted() {
        return find("deletedAt is not null", Sort.by("deletedAt").descending())
                .list();
    }

    // ==================== Métodos Privados de Utilidad ====================

    /**
     * Aplica los filtros de ProductUomFilter a la query.
     */
    private void applyFilters(StringBuilder query, Map<String, Object> params, ProductUomFilter filter) {
        if (filter == null) {
            query.append(" and u.deletedAt is null");
            return;
        }

        // Filtro: incluir/excluir eliminados
        if ("1".equals(filter.getIncludeDeleted())) {
            // Incluir eliminados: no agregar condición de deletedAt
        } else {
            query.append(" and u.deletedAt is null");
        }

        // Filtro: búsqueda general en código o nombre
        if (filter.getSearch() != null && !filter.getSearch().isBlank()) {
            query.append(" and (lower(u.codeUom) like :search or lower(u.nameUom) like :search)");
            params.put("search", "%" + filter.getSearch().toLowerCase().trim() + "%");
        }

        // Filtro: código exacto
        if (filter.getCodeUom() != null && !filter.getCodeUom().isBlank()) {
            query.append(" and u.codeUom = :codeUom");
            params.put("codeUom", filter.getCodeUom().trim());
        }

        // Filtro: nombre exacto
        if (filter.getNameUom() != null && !filter.getNameUom().isBlank()) {
            query.append(" and lower(u.nameUom) = :nameUom");
            params.put("nameUom", filter.getNameUom().toLowerCase().trim());
        }

        // Filtro: categoría
        if (filter.getCategoryId() != null) {
            query.append(" and u.categoryId = :categoryId");
            params.put("categoryId", filter.getCategoryId());
        }

        // Filtro: estado activo/inactivo
        if (filter.getActive() != null && !filter.getActive().isBlank()) {
            boolean isActive = "1".equals(filter.getActive());
            query.append(" and u.active = :active");
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
            return "nameUom"; // Default
        }

        return switch (field.toLowerCase().trim()) {
            case "id" -> "id";
            case "code", "code_uom", "codeuom" -> "codeUom";
            case "name", "name_uom", "nameuom" -> "nameUom";
            case "category", "category_id", "categoryid" -> "categoryId";
            case "factor" -> "factor";
            case "rounding", "rounding_precision", "roundingprecision" -> "roundingPrecision";
            case "active", "is_active" -> "active";
            case "createdat", "created_at" -> "createdAt";
            case "updatedat", "updated_at" -> "updatedAt";
            default -> "nameUom"; // Default seguro
        };
    }
}
