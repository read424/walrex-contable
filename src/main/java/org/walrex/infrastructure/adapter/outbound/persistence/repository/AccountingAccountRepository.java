package org.walrex.infrastructure.adapter.outbound.persistence.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.walrex.application.dto.query.AccountingAccountFilter;
import org.walrex.application.dto.query.PageRequest;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.AccountingAccountEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repositorio para operaciones de persistencia de AccountingAccount.
 *
 * Usa el patrón Repository de Panache que proporciona:
 * - Operaciones CRUD básicas
 * - Queries tipadas
 * - Paginación integrada
 */
@ApplicationScoped
public class AccountingAccountRepository implements PanacheRepositoryBase<AccountingAccountEntity, Integer> {

    /**
     * Busca cuenta activa (no eliminada) por ID.
     */
    public Uni<AccountingAccountEntity> findActiveById(Integer id) {
        return find("id = ?1 and deletedAt is null", id).firstResult();
    }

    /**
     * Busca cuenta por código (activas solamente).
     */
    public Uni<AccountingAccountEntity> findByCode(String code) {
        return find("code = ?1 and deletedAt is null", code.trim())
                .firstResult();
    }

    /**
     * Busca cuenta por nombre exacto (activas solamente).
     */
    public Uni<AccountingAccountEntity> findByName(String name) {
        return find("lower(name) = ?1 and deletedAt is null", name.toLowerCase().trim())
                .firstResult();
    }

    // ==================== Verificaciones de Unicidad ====================

    /**
     * Verifica si existe una cuenta con el código.
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
     * Verifica si existe una cuenta con el nombre.
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
     * Lista cuentas con paginación, ordenamiento y filtros.
     */
    public Multi<AccountingAccountEntity> findWithFilters(PageRequest pageRequest, AccountingAccountFilter filter) {
        StringBuilder query = new StringBuilder("1=1");
        Map<String, Object> params = new HashMap<>();

        // Aplicar filtros del objeto AccountingAccountFilter
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

    public Multi<AccountingAccountEntity> findAllWithFilters(AccountingAccountFilter accountFilter){
        StringBuilder query = new StringBuilder("1=1");
        Map<String, Object> params = new HashMap<>();

        // Aplicar filtros del objeto AccountingAccountFilter
        applyFilters(query, params, accountFilter);

        // Construir Sort
        Sort sort = Sort.by("code", Sort.Direction.Ascending);

        return find(query.toString(), sort, params)
                .list()
                .onItem()
                .transformToMulti(list -> Multi.createFrom().iterable(list));

    }

    /**
     * Cuenta el total de cuentas que cumplen los filtros.
     */
    public Uni<Long> countWithFilters(AccountingAccountFilter filter) {
        StringBuilder query = new StringBuilder("1=1");
        Map<String, Object> params = new HashMap<>();

        applyFilters(query, params, filter);

        return count(query.toString(), params);
    }

    /**
     * Stream de todas las cuentas activas.
     */
    public Multi<AccountingAccountEntity> streamAll() {
        return list("deletedAt is null", Sort.by("name").ascending())
                .onItem()
                .transformToMulti(list-> Multi.createFrom().iterable(list));
    }

    /**
     * Lista todas las cuentas eliminadas.
     */
    public Uni<List<AccountingAccountEntity>> findAllDeleted() {
        return find("deletedAt is not null", Sort.by("deletedAt").descending())
                .list();
    }

    // ==================== Métodos Privados de Utilidad ====================

    /**
     * Aplica los filtros de AccountingAccountFilter a la query.
     */
    private void applyFilters(StringBuilder query, Map<String, Object> params, AccountingAccountFilter filter) {
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

        // Filtro: tipo de cuenta (ASSET, LIABILITY, etc.)
        if (filter.getType() != null && !filter.getType().isBlank()) {
            query.append(" and type = :type");
            params.put("type", filter.getType().toUpperCase().trim());
        }

        // Filtro: lado normal (DEBIT, CREDIT)
        if (filter.getNormalSide() != null && !filter.getNormalSide().isBlank()) {
            query.append(" and normalSide = :normalSide");
            params.put("normalSide", filter.getNormalSide().toUpperCase().trim());
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
            case "type" -> "type";
            case "normalside", "normal_side" -> "normalSide";
            case "active", "is_active" -> "active";
            case "createdat", "created_at" -> "createdAt";
            case "updatedat", "updated_at" -> "updatedAt";
            default -> "name"; // Default seguro
        };
    }
}
