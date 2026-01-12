package org.walrex.infrastructure.adapter.outbound.persistence.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.walrex.application.dto.query.PageRequest;
import org.walrex.application.dto.query.ProductAttributeValueFilter;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.ProductAttributeValueEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repositorio para operaciones de persistencia de ProductAttributeValue.
 *
 * Usa el patrón Repository de Panache que proporciona:
 * - Operaciones CRUD básicas
 * - Queries tipadas
 * - Paginación integrada
 * - JOIN FETCH para cargar el atributo relacionado y evitar N+1 queries
 *
 * IMPORTANTE: Este repositorio usa Integer como tipo de ID.
 * PanacheRepositoryBase<ProductAttributeValueEntity, Integer>
 */
@ApplicationScoped
public class ProductAttributeValueRepository implements PanacheRepositoryBase<ProductAttributeValueEntity, Integer> {

    /**
     * Busca valor de atributo activo (no eliminado) por ID.
     * Incluye JOIN FETCH para cargar el atributo relacionado.
     */
    public Uni<ProductAttributeValueEntity> findActiveById(Integer id) {
        return find("SELECT v FROM ProductAttributeValueEntity v LEFT JOIN FETCH v.attribute " +
                "WHERE v.id = ?1 AND v.deletedAt IS NULL", id)
                .firstResult();
    }

    /**
     * Busca valor de atributo por nombre exacto (activos solamente).
     */
    public Uni<ProductAttributeValueEntity> findByName(String name) {
        return find("lower(name) = ?1 and deletedAt is null", name.toLowerCase().trim())
                .firstResult();
    }

    /**
     * Busca todos los valores de un atributo específico.
     * Incluye JOIN FETCH para cargar el atributo relacionado.
     */
    public Uni<List<ProductAttributeValueEntity>> findByAttributeId(Integer attributeId) {
        return find("SELECT v FROM ProductAttributeValueEntity v LEFT JOIN FETCH v.attribute " +
                        "WHERE v.attributeId = ?1 AND v.deletedAt IS NULL ORDER BY v.sequence ASC, v.name ASC",
                attributeId)
                .list();
    }

    // ==================== Verificaciones de Unicidad ====================

    /**
     * Verifica si existe un valor de atributo con el ID.
     * Opcionalmente excluye un ID (útil para updates).
     */
    public Uni<Boolean> existsById(Integer id, Integer excludeId) {
        if (excludeId == null) {
            return count("id = ?1 and deletedAt is null", id)
                    .map(count -> count > 0);
        }
        return count("id = ?1 and deletedAt is null and id != ?2", id, excludeId)
                .map(count -> count > 0);
    }

    /**
     * Verifica si existe un valor de atributo con la combinación (attributeId, name).
     * La combinación debe ser única según constraint uk_attribute_value.
     */
    public Uni<Boolean> existsByAttributeIdAndName(Integer attributeId, String name, Integer excludeId) {
        String normalizedName = name.toLowerCase().trim();

        if (excludeId == null) {
            return count("attributeId = ?1 and lower(name) = ?2 and deletedAt is null",
                    attributeId, normalizedName)
                    .map(count -> count > 0);
        }
        return count("attributeId = ?1 and lower(name) = ?2 and deletedAt is null and id != ?3",
                attributeId, normalizedName, excludeId)
                .map(count -> count > 0);
    }

    // ==================== Listados con Paginación y Filtros ====================

    /**
     * Lista valores de atributos con paginación, ordenamiento y filtros.
     * IMPORTANTE: Incluye JOIN FETCH para cargar el atributo relacionado.
     */
    public Multi<ProductAttributeValueEntity> findWithFilters(PageRequest pageRequest, ProductAttributeValueFilter filter) {
        StringBuilder query = new StringBuilder(
                "SELECT v FROM ProductAttributeValueEntity v LEFT JOIN FETCH v.attribute WHERE 1=1");
        Map<String, Object> params = new HashMap<>();

        // Aplicar filtros del objeto ProductAttributeValueFilter
        applyFilters(query, params, filter);

        // Agregar ORDER BY directamente en la query con alias cualificado
        String orderByClause = buildOrderByClause(pageRequest);
        query.append(orderByClause);

        // Construir Page
        Page page = Page.of(pageRequest.getPage(), pageRequest.getSize());

        // Ejecutar query con paginación (sin Sort object, ya incluido en query)
        return find(query.toString(), params)
                .page(page)
                .list()
                .onItem()
                .transformToMulti(list -> Multi.createFrom().iterable(list));
    }

    /**
     * Lista todos los valores de atributos con filtros (sin paginación).
     * IMPORTANTE: Incluye JOIN FETCH para cargar el atributo relacionado.
     */
    public Multi<ProductAttributeValueEntity> findAllWithFilters(ProductAttributeValueFilter filter) {
        StringBuilder query = new StringBuilder(
                "SELECT v FROM ProductAttributeValueEntity v LEFT JOIN FETCH v.attribute WHERE 1=1");
        Map<String, Object> params = new HashMap<>();

        // Aplicar filtros del objeto ProductAttributeValueFilter
        applyFilters(query, params, filter);

        // Agregar ORDER BY directamente en la query para evitar ambigüedad
        query.append(" ORDER BY v.name ASC");

        return find(query.toString(), params)
                .list()
                .onItem()
                .transformToMulti(list -> Multi.createFrom().iterable(list));
    }

    /**
     * Cuenta el total de valores de atributos que cumplen los filtros.
     */
    public Uni<Long> countWithFilters(ProductAttributeValueFilter filter) {
        StringBuilder query = new StringBuilder("1=1");
        Map<String, Object> params = new HashMap<>();

        applyFilters(query, params, filter);

        return count(query.toString(), params);
    }

    /**
     * Stream de todos los valores de atributos activos.
     * Incluye JOIN FETCH para cargar el atributo relacionado.
     */
    public Multi<ProductAttributeValueEntity> streamAll() {
        return list("SELECT v FROM ProductAttributeValueEntity v LEFT JOIN FETCH v.attribute " +
                        "WHERE v.deletedAt IS NULL ORDER BY v.name ASC")
                .onItem()
                .transformToMulti(list -> Multi.createFrom().iterable(list));
    }

    /**
     * Lista todos los valores de atributos eliminados.
     */
    public Uni<List<ProductAttributeValueEntity>> findAllDeleted() {
        return find("deletedAt is not null", Sort.by("deletedAt").descending())
                .list();
    }

    // ==================== Métodos Privados de Utilidad ====================

    /**
     * Aplica los filtros de ProductAttributeValueFilter a la query.
     */
    private void applyFilters(StringBuilder query, Map<String, Object> params, ProductAttributeValueFilter filter) {
        if (filter == null) {
            query.append(" and v.deletedAt is null");
            return;
        }

        // Filtro: incluir/excluir eliminados
        if ("1".equals(filter.getIncludeDeleted())) {
            // Incluir eliminados: no agregar condición de deletedAt
        } else {
            query.append(" and v.deletedAt is null");
        }

        // Filtro: búsqueda general en id o nombre
        if (filter.getSearch() != null && !filter.getSearch().isBlank()) {
            query.append(" and (lower(v.id) like :search or lower(v.name) like :search)");
            params.put("search", "%" + filter.getSearch().toLowerCase().trim() + "%");
        }

        // Filtro: nombre exacto
        if (filter.getName() != null && !filter.getName().isBlank()) {
            query.append(" and lower(v.name) = :name");
            params.put("name", filter.getName().toLowerCase().trim());
        }

        // Filtro: atributo
        if (filter.getAttributeId() != null) {
            query.append(" and v.attributeId = :attributeId");
            params.put("attributeId", filter.getAttributeId());
        }

        // Filtro: estado activo/inactivo
        if (filter.getActive() != null && !filter.getActive().isBlank()) {
            boolean isActive = "1".equals(filter.getActive());
            query.append(" and v.active = :active");
            params.put("active", isActive);
        }
    }

    /**
     * Construye la cláusula ORDER BY completa con alias cualificado.
     * Necesario para evitar ambigüedad cuando hay JOIN FETCH con múltiples entidades que tienen campos con el mismo nombre.
     */
    private String buildOrderByClause(PageRequest pageRequest) {
        String sortField = validateAndNormalizeSortField(pageRequest.getSortBy());
        String direction = pageRequest.getSortDirection() == PageRequest.SortDirection.DESCENDING ? "DESC" : "ASC";

        return " ORDER BY v." + sortField + " " + direction;
    }

    /**
     * Valida y normaliza el campo de ordenamiento para prevenir SQL injection.
     * Retorna solo el nombre del campo (sin alias).
     */
    private String validateAndNormalizeSortField(String field) {
        if (field == null || field.isBlank()) {
            return "name"; // Default
        }

        return switch (field.toLowerCase().trim()) {
            case "id" -> "id";
            case "name" -> "name";
            case "attribute", "attribute_id", "attributeid" -> "attributeId";
            case "sequence" -> "sequence";
            case "active", "is_active" -> "active";
            case "createdat", "created_at" -> "createdAt";
            case "updatedat", "updated_at" -> "updatedAt";
            default -> "name"; // Default seguro
        };
    }
}
