package org.walrex.infrastructure.adapter.outbound.persistence.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.walrex.application.dto.query.PageRequest;
import org.walrex.application.dto.query.ProductAttributeFilter;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.ProductAttributeEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repositorio para operaciones de persistencia de ProductAttribute.
 *
 * Usa el patrón Repository de Panache que proporciona:
 * - Operaciones CRUD básicas
 * - Queries tipadas
 * - Paginación integrada
 *
 * IMPORTANTE: Este repositorio usa Integer como tipo de ID (auto-generado).
 * PanacheRepositoryBase<ProductAttributeEntity, Integer>
 */
@ApplicationScoped
public class ProductAttributeRepository implements PanacheRepositoryBase<ProductAttributeEntity, Integer> {

    /**
     * Busca atributo activo (no eliminado) por ID.
     */
    public Uni<ProductAttributeEntity> findActiveById(Integer id) {
        return find("id = ?1 and deletedAt is null", id).firstResult();
    }

    /**
     * Busca atributo por nombre exacto (activos solamente).
     */
    public Uni<ProductAttributeEntity> findByName(String name) {
        return find("lower(name) = ?1 and deletedAt is null", name.toLowerCase().trim())
                .firstResult();
    }

    // ==================== Verificaciones de Unicidad ====================

    /**
     * Verifica si existe un atributo con el nombre.
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
     * Lista atributos con paginación, ordenamiento y filtros.
     */
    public Multi<ProductAttributeEntity> findWithFilters(PageRequest pageRequest, ProductAttributeFilter filter) {
        StringBuilder query = new StringBuilder("1=1");
        Map<String, Object> params = new HashMap<>();

        // Aplicar filtros del objeto ProductAttributeFilter
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
     * Lista todos los atributos con filtros (sin paginación).
     */
    public Multi<ProductAttributeEntity> findAllWithFilters(ProductAttributeFilter filter) {
        StringBuilder query = new StringBuilder("1=1");
        Map<String, Object> params = new HashMap<>();

        // Aplicar filtros del objeto ProductAttributeFilter
        applyFilters(query, params, filter);

        // Construir Sort
        Sort sort = Sort.by("name", Sort.Direction.Ascending);

        return find(query.toString(), sort, params)
                .list()
                .onItem()
                .transformToMulti(list -> Multi.createFrom().iterable(list));
    }

    /**
     * Cuenta el total de atributos que cumplen los filtros.
     */
    public Uni<Long> countWithFilters(ProductAttributeFilter filter) {
        StringBuilder query = new StringBuilder("1=1");
        Map<String, Object> params = new HashMap<>();

        applyFilters(query, params, filter);

        return count(query.toString(), params);
    }

    /**
     * Stream de todos los atributos activos.
     */
    public Multi<ProductAttributeEntity> streamAll() {
        return list("deletedAt is null", Sort.by("name").ascending())
                .onItem()
                .transformToMulti(list -> Multi.createFrom().iterable(list));
    }

    /**
     * Lista todos los atributos eliminados.
     */
    public Uni<List<ProductAttributeEntity>> findAllDeleted() {
        return find("deletedAt is not null", Sort.by("deletedAt").descending())
                .list();
    }

    // ==================== Métodos Privados de Utilidad ====================

    /**
     * Aplica los filtros de ProductAttributeFilter a la query.
     */
    private void applyFilters(StringBuilder query, Map<String, Object> params, ProductAttributeFilter filter) {
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

        // Filtro: búsqueda general en id o nombre
        if (filter.getSearch() != null && !filter.getSearch().isBlank()) {
            query.append(" and (lower(id) like :search or lower(name) like :search)");
            params.put("search", "%" + filter.getSearch().toLowerCase().trim() + "%");
        }

        // Filtro: nombre exacto
        if (filter.getName() != null && !filter.getName().isBlank()) {
            query.append(" and lower(name) = :name");
            params.put("name", filter.getName().toLowerCase().trim());
        }

        // Filtro: tipo de visualización
        if (filter.getDisplayType() != null) {
            query.append(" and displayType = :displayType");
            params.put("displayType", filter.getDisplayType());
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
            case "name" -> "name";
            case "displaytype", "display_type" -> "displayType";
            case "active", "is_active" -> "active";
            case "createdat", "created_at" -> "createdAt";
            case "updatedat", "updated_at" -> "updatedAt";
            default -> "name"; // Default seguro
        };
    }
}
