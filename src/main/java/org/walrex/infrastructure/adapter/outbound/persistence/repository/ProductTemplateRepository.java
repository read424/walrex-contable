package org.walrex.infrastructure.adapter.outbound.persistence.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hibernate.reactive.mutiny.Mutiny;
import org.walrex.application.dto.query.PageRequest;
import org.walrex.application.dto.query.ProductTemplateFilter;
import org.walrex.domain.model.ProductType;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.ProductTemplateEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repositorio para operaciones de persistencia de ProductTemplate.
 *
 * Usa el patrón Repository de Panache que proporciona:
 * - Operaciones CRUD básicas
 * - Queries tipadas
 * - Paginación integrada
 * - JOIN FETCH para cargar las entidades relacionadas y evitar N+1 queries
 *
 * IMPORTANTE: Todas las queries con relaciones usan:
 * - JOIN FETCH para category, brand, uom, currency
 * - Alias "pt" para ProductTemplateEntity
 * - Campos calificados con alias en ORDER BY (pt.name, pt.id, etc.)
 */
@ApplicationScoped
public class ProductTemplateRepository implements PanacheRepositoryBase<ProductTemplateEntity, Integer> {

    @Inject
    Mutiny.SessionFactory sessionFactory;

    /**
     * Busca plantilla de producto activa (no eliminada) por ID.
     * Incluye JOIN FETCH para cargar las entidades relacionadas.
     */
    public Uni<ProductTemplateEntity> findActiveById(Integer id) {
        return find("SELECT pt FROM ProductTemplateEntity pt " +
                "LEFT JOIN FETCH pt.category " +
                "LEFT JOIN FETCH pt.brand " +
                "LEFT JOIN FETCH pt.uom " +
                "LEFT JOIN FETCH pt.currency " +
                "WHERE pt.id = ?1 AND pt.deletedAt IS NULL", id)
                .firstResult();
    }

    /**
     * Busca plantilla de producto por referencia interna (activas solamente).
     */
    public Uni<ProductTemplateEntity> findByInternalReference(String internalReference) {
        return find("internalReference = ?1 and deletedAt is null", internalReference.trim().toUpperCase())
                .firstResult();
    }

    // ==================== Verificaciones de Unicidad ====================

    /**
     * Verifica si existe una plantilla con la referencia interna.
     * Opcionalmente excluye un ID (útil para updates).
     */
    public Uni<Boolean> existsByInternalReference(String internalReference, Integer excludeId) {
        String normalizedRef = internalReference.trim().toUpperCase();
        if (excludeId == null) {
            return count("internalReference = ?1 and deletedAt is null", normalizedRef)
                    .map(count -> count > 0);
        }
        return count("internalReference = ?1 and deletedAt is null and id != ?2",
                normalizedRef, excludeId)
                .map(count -> count > 0);
    }

    // ==================== Listados con Paginación y Filtros ====================

    /**
     * Lista plantillas de producto con paginación, ordenamiento y filtros.
     * IMPORTANTE: Incluye JOIN FETCH para cargar las entidades relacionadas.
     */
    public Multi<ProductTemplateEntity> findWithFilters(PageRequest pageRequest, ProductTemplateFilter filter) {
        StringBuilder query = new StringBuilder(
                "SELECT pt FROM ProductTemplateEntity pt " +
                "LEFT JOIN FETCH pt.category " +
                "LEFT JOIN FETCH pt.brand " +
                "LEFT JOIN FETCH pt.uom " +
                "LEFT JOIN FETCH pt.currency " +
                "WHERE 1=1");
        Map<String, Object> params = new HashMap<>();

        // Aplicar filtros del objeto ProductTemplateFilter
        applyFilters(query, params, filter);

        // Construir ordenamiento con alias calificado
        String orderByClause = buildOrderByClause(pageRequest);

        // Construir Page
        Page page = Page.of(pageRequest.getPage(), pageRequest.getSize());

        // Ejecutar query con paginación
        // NOTA: No podemos usar Sort con JOIN FETCH, usamos ORDER BY en el query
        return find(query.toString() + orderByClause, params)
                .page(page)
                .list()
                .onItem()
                .transformToMulti(list -> Multi.createFrom().iterable(list));
    }

    /**
     * Lista todas las plantillas de producto con filtros (sin paginación).
     * IMPORTANTE: Incluye JOIN FETCH para cargar las entidades relacionadas.
     */
    public Multi<ProductTemplateEntity> findAllWithFilters(ProductTemplateFilter filter) {
        StringBuilder query = new StringBuilder(
                "SELECT pt FROM ProductTemplateEntity pt " +
                "LEFT JOIN FETCH pt.category " +
                "LEFT JOIN FETCH pt.brand " +
                "LEFT JOIN FETCH pt.uom " +
                "LEFT JOIN FETCH pt.currency " +
                "WHERE 1=1");
        Map<String, Object> params = new HashMap<>();

        // Aplicar filtros del objeto ProductTemplateFilter
        applyFilters(query, params, filter);

        // Ordenamiento por defecto
        query.append(" ORDER BY pt.name ASC");

        return find(query.toString(), params)
                .list()
                .onItem()
                .transformToMulti(list -> Multi.createFrom().iterable(list));
    }

    /**
     * Cuenta el total de plantillas de producto que cumplen los filtros.
     */
    public Uni<Long> countWithFilters(ProductTemplateFilter filter) {
        StringBuilder query = new StringBuilder("1=1");
        Map<String, Object> params = new HashMap<>();

        applyFilters(query, params, filter);

        return count(query.toString(), params);
    }

    /**
     * Stream de todas las plantillas activas.
     * Incluye JOIN FETCH para cargar las entidades relacionadas.
     */
    public Multi<ProductTemplateEntity> streamAll() {
        return list("SELECT pt FROM ProductTemplateEntity pt " +
                        "LEFT JOIN FETCH pt.category " +
                        "LEFT JOIN FETCH pt.brand " +
                        "LEFT JOIN FETCH pt.uom " +
                        "LEFT JOIN FETCH pt.currency " +
                        "WHERE pt.deletedAt IS NULL ORDER BY pt.name ASC")
                .onItem()
                .transformToMulti(list -> Multi.createFrom().iterable(list));
    }

    /**
     * Lista todas las plantillas eliminadas.
     */
    public Uni<List<ProductTemplateEntity>> findAllDeleted() {
        return find("deletedAt is not null", Sort.by("deletedAt").descending())
                .list();
    }

    // ==================== Métodos Privados de Utilidad ====================

    /**
     * Aplica los filtros de ProductTemplateFilter a la query.
     */
    private void applyFilters(StringBuilder query, Map<String, Object> params, ProductTemplateFilter filter) {
        if (filter == null) {
            query.append(" and pt.deletedAt is null");
            return;
        }

        // Filtro: incluir/excluir eliminados
        if ("1".equals(filter.getIncludeDeleted())) {
            // Incluir eliminados: no agregar condición de deletedAt
        } else {
            query.append(" and pt.deletedAt is null");
        }

        // Filtro: búsqueda general en nombre, referencia interna, descripción
        if (filter.getSearch() != null && !filter.getSearch().isBlank()) {
            query.append(" and (lower(pt.name) like :search " +
                    "or lower(pt.internalReference) like :search " +
                    "or lower(pt.description) like :search)");
            params.put("search", "%" + filter.getSearch().toLowerCase().trim() + "%");
        }

        // Filtro: nombre exacto
        if (filter.getName() != null && !filter.getName().isBlank()) {
            query.append(" and lower(pt.name) = :name");
            params.put("name", filter.getName().toLowerCase().trim());
        }

        // Filtro: referencia interna exacta
        if (filter.getInternalReference() != null && !filter.getInternalReference().isBlank()) {
            query.append(" and pt.internalReference = :internalReference");
            params.put("internalReference", filter.getInternalReference().trim().toUpperCase());
        }

        // Filtro: tipo de producto
        if (filter.getType() != null) {
            query.append(" and pt.type = :type");
            params.put("type", filter.getType());
        }

        // Filtro: categoría
        if (filter.getCategoryId() != null) {
            query.append(" and pt.categoryId = :categoryId");
            params.put("categoryId", filter.getCategoryId());
        }

        // Filtro: marca
        if (filter.getBrandId() != null) {
            query.append(" and pt.brandId = :brandId");
            params.put("brandId", filter.getBrandId());
        }

        // Filtro: estado
        if (filter.getStatus() != null && !filter.getStatus().isBlank()) {
            query.append(" and pt.status = :status");
            params.put("status", filter.getStatus().toLowerCase().trim());
        }

        // Filtro: puede ser vendido
        if (filter.getCanBeSold() != null && !filter.getCanBeSold().isBlank()) {
            boolean canBeSold = "1".equals(filter.getCanBeSold());
            query.append(" and pt.canBeSold = :canBeSold");
            params.put("canBeSold", canBeSold);
        }

        // Filtro: puede ser comprado
        if (filter.getCanBePurchased() != null && !filter.getCanBePurchased().isBlank()) {
            boolean canBePurchased = "1".equals(filter.getCanBePurchased());
            query.append(" and pt.canBePurchased = :canBePurchased");
            params.put("canBePurchased", canBePurchased);
        }

        // Filtro: tiene variantes
        if (filter.getHasVariants() != null && !filter.getHasVariants().isBlank()) {
            boolean hasVariants = "1".equals(filter.getHasVariants());
            query.append(" and pt.hasVariants = :hasVariants");
            params.put("hasVariants", hasVariants);
        }
    }

    /**
     * Construye la cláusula ORDER BY con alias calificado basado en PageRequest.
     */
    private String buildOrderByClause(PageRequest pageRequest) {
        String sortField = validateAndNormalizeSortField(pageRequest.getSortBy());
        String direction = pageRequest.getSortDirection() == PageRequest.SortDirection.DESCENDING
                ? "DESC"
                : "ASC";

        return " ORDER BY pt." + sortField + " " + direction;
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
            case "internal_reference", "internalreference" -> "internalReference";
            case "type", "type_product", "typeproduct" -> "type";
            case "category", "category_id", "categoryid" -> "categoryId";
            case "brand", "brand_id", "brandid" -> "brandId";
            case "uom", "uom_id", "uomid" -> "uomId";
            case "currency", "currency_id", "currencyid" -> "currencyId";
            case "sale_price", "saleprice" -> "salePrice";
            case "cost" -> "cost";
            case "status" -> "status";
            case "createdat", "created_at" -> "createdAt";
            case "updatedat", "updated_at" -> "updatedAt";
            default -> "name"; // Default seguro
        };
    }

    // ==================== Queries SQL Nativas ====================

    /**
     * Lista plantillas de producto usando SQL nativo con LEFT OUTER JOIN.
     *
     * Evita problemas de Hibernate Reactive al usar SQL directo.
     * Retorna solo los campos necesarios para el listado paginado.
     */
    public Uni<List<Map<String, Object>>> findWithFiltersNative(PageRequest pageRequest, ProductTemplateFilter filter) {
        StringBuilder sql = new StringBuilder(
                "SELECT " +
                "pt.id, " +
                "pt.name, " +
                "pt.internal_reference, " +
                "pt.type_product, " +
                "pt.id_category, " +
                "pc.name as category_name, " +
                "pt.id_brand, " +
                "pb.name as brand_name, " +
                "pt.id_uom, " +
                "pu.name_uom as uom_name, " +
                "pu.code_uom as uom_code, " +
                "pt.id_currency, " +
                "c.code_iso3 as currency_code, " +
                "c.symbol as currency_symbol, " +
                "pt.sale_price, " +
                "pt.cost, " +
                "pt.is_igv_exempt, " +
                "pt.tax_rate, " +
                "pt.weight, " +
                "pt.volume, " +
                "pt.track_inventory, " +
                "pt.use_serial_numbers, " +
                "pt.minimum_stock, " +
                "pt.maximum_stock, " +
                "pt.reorder_point, " +
                "pt.lead_time, " +
                "pt.image, " +
                "pt.description, " +
                "pt.description_sale, " +
                "pt.barcode, " +
                "pt.notes, " +
                "pt.can_be_sold, " +
                "pt.can_be_purchased, " +
                "pt.allows_price_edit, " +
                "pt.has_variants, " +
                "pt.status, " +
                "pt.created_at, " +
                "pt.updated_at " +
                "FROM product_templates pt " +
                "LEFT OUTER JOIN product_category pc ON pt.id_category = pc.id " +
                "LEFT OUTER JOIN product_brand pb ON pt.id_brand = pb.id " +
                "LEFT OUTER JOIN product_uom pu ON pt.id_uom = pu.id " +
                "LEFT OUTER JOIN currencies c ON pt.id_currency = c.id " +
                "WHERE 1=1"
        );

        Map<String, Object> params = new HashMap<>();

        // Aplicar filtros
        applyFiltersNative(sql, params, filter);

        // Ordenamiento
        String orderByClause = buildOrderByClauseNative(pageRequest);
        sql.append(orderByClause);

        // Paginación
        int offset = pageRequest.getPage() * pageRequest.getSize();
        sql.append(" LIMIT :limit OFFSET :offset");
        params.put("limit", pageRequest.getSize());
        params.put("offset", offset);

        return sessionFactory.withSession(session -> {
            var query = session.createNativeQuery(sql.toString());

            // Establecer parámetros
            params.forEach(query::setParameter);

            return query.getResultList()
                    .map(resultList -> {
                        @SuppressWarnings("unchecked")
                        List<Object> rawRows = (List<Object>) resultList;
                        return rawRows.stream()
                                .map(obj -> (Object[]) obj)
                                .map(this::mapRowToResult)
                                .toList();
                    });
        });
    }

    /**
     * Cuenta el total de plantillas con SQL nativo.
     */
    public Uni<Long> countWithFiltersNative(ProductTemplateFilter filter) {
        StringBuilder sql = new StringBuilder(
                "SELECT COUNT(*) " +
                "FROM product_templates pt " +
                "WHERE 1=1"
        );

        Map<String, Object> params = new HashMap<>();
        applyFiltersNative(sql, params, filter);

        return sessionFactory.withSession(session -> {
            var query = session.createNativeQuery(sql.toString());
            params.forEach(query::setParameter);

            return query.getSingleResult()
                    .map(result -> ((Number) result).longValue());
        });
    }

    /**
     * Mapea una fila de resultado SQL a un Map.
     */
    private Map<String, Object> mapRowToResult(Object[] row) {
        Map<String, Object> result = new HashMap<>();

        int i = 0;
        result.put("id", row[i++]);
        result.put("name", row[i++]);
        result.put("internalReference", row[i++]);
        result.put("type", row[i++]);
        result.put("categoryId", row[i++]);
        result.put("categoryName", row[i++]);
        result.put("brandId", row[i++]);
        result.put("brandName", row[i++]);
        result.put("uomId", row[i++]);
        result.put("uomName", row[i++]);
        result.put("uomCode", row[i++]);
        result.put("currencyId", row[i++]);
        result.put("currencyCode", row[i++]);
        result.put("currencySymbol", row[i++]);
        result.put("salePrice", row[i++]);
        result.put("cost", row[i++]);
        result.put("isIgvExempt", row[i++]);
        result.put("taxRate", row[i++]);
        result.put("weight", row[i++]);
        result.put("volume", row[i++]);
        result.put("trackInventory", row[i++]);
        result.put("useSerialNumbers", row[i++]);
        result.put("minimumStock", row[i++]);
        result.put("maximumStock", row[i++]);
        result.put("reorderPoint", row[i++]);
        result.put("leadTime", row[i++]);
        result.put("image", row[i++]);
        result.put("description", row[i++]);
        result.put("descriptionSale", row[i++]);
        result.put("barcode", row[i++]);
        result.put("notes", row[i++]);
        result.put("canBeSold", row[i++]);
        result.put("canBePurchased", row[i++]);
        result.put("allowsPriceEdit", row[i++]);
        result.put("hasVariants", row[i++]);
        result.put("status", row[i++]);
        result.put("createdAt", row[i++]);
        result.put("updatedAt", row[i++]);

        return result;
    }

    /**
     * Aplica filtros en SQL nativo.
     */
    private void applyFiltersNative(StringBuilder sql, Map<String, Object> params, ProductTemplateFilter filter) {
        if (filter == null) {
            sql.append(" AND pt.deleted_at IS NULL");
            return;
        }

        // Filtro: incluir/excluir eliminados
        if (!"1".equals(filter.getIncludeDeleted())) {
            sql.append(" AND pt.deleted_at IS NULL");
        }

        // Filtro: búsqueda general
        if (filter.getSearch() != null && !filter.getSearch().isBlank()) {
            sql.append(" AND (LOWER(pt.name) LIKE :search " +
                    "OR LOWER(pt.internal_reference) LIKE :search " +
                    "OR LOWER(pt.description) LIKE :search)");
            params.put("search", "%" + filter.getSearch().toLowerCase().trim() + "%");
        }

        // Filtro: nombre exacto
        if (filter.getName() != null && !filter.getName().isBlank()) {
            sql.append(" AND LOWER(pt.name) = :name");
            params.put("name", filter.getName().toLowerCase().trim());
        }

        // Filtro: referencia interna
        if (filter.getInternalReference() != null && !filter.getInternalReference().isBlank()) {
            sql.append(" AND pt.internal_reference = :internalReference");
            params.put("internalReference", filter.getInternalReference().trim().toUpperCase());
        }

        // Filtro: tipo de producto
        if (filter.getType() != null) {
            sql.append(" AND pt.type_product = :type");
            params.put("type", filter.getType().getValue());
        }

        // Filtro: categoría
        if (filter.getCategoryId() != null) {
            sql.append(" AND pt.id_category = :categoryId");
            params.put("categoryId", filter.getCategoryId());
        }

        // Filtro: marca
        if (filter.getBrandId() != null) {
            sql.append(" AND pt.id_brand = :brandId");
            params.put("brandId", filter.getBrandId());
        }

        // Filtro: estado
        if (filter.getStatus() != null && !filter.getStatus().isBlank()) {
            sql.append(" AND pt.status = :status");
            params.put("status", filter.getStatus().toLowerCase().trim());
        }

        // Filtro: puede ser vendido
        if (filter.getCanBeSold() != null && !filter.getCanBeSold().isBlank()) {
            boolean canBeSold = "1".equals(filter.getCanBeSold());
            sql.append(" AND pt.can_be_sold = :canBeSold");
            params.put("canBeSold", canBeSold);
        }

        // Filtro: puede ser comprado
        if (filter.getCanBePurchased() != null && !filter.getCanBePurchased().isBlank()) {
            boolean canBePurchased = "1".equals(filter.getCanBePurchased());
            sql.append(" AND pt.can_be_purchased = :canBePurchased");
            params.put("canBePurchased", canBePurchased);
        }

        // Filtro: tiene variantes
        if (filter.getHasVariants() != null && !filter.getHasVariants().isBlank()) {
            boolean hasVariants = "1".equals(filter.getHasVariants());
            sql.append(" AND pt.has_variants = :hasVariants");
            params.put("hasVariants", hasVariants);
        }
    }

    /**
     * Construye ORDER BY para SQL nativo.
     */
    private String buildOrderByClauseNative(PageRequest pageRequest) {
        String sortField = validateAndNormalizeSortFieldNative(pageRequest.getSortBy());
        String direction = pageRequest.getSortDirection() == PageRequest.SortDirection.DESCENDING
                ? "DESC"
                : "ASC";

        return " ORDER BY pt." + sortField + " " + direction;
    }

    /**
     * Valida campo de ordenamiento para SQL nativo.
     */
    private String validateAndNormalizeSortFieldNative(String field) {
        if (field == null || field.isBlank()) {
            return "name";
        }

        return switch (field.toLowerCase().trim()) {
            case "id" -> "id";
            case "name" -> "name";
            case "internal_reference", "internalreference" -> "internal_reference";
            case "type", "type_product", "typeproduct" -> "type_product";
            case "category", "category_id", "categoryid" -> "id_category";
            case "brand", "brand_id", "brandid" -> "id_brand";
            case "uom", "uom_id", "uomid" -> "id_uom";
            case "currency", "currency_id", "currencyid" -> "id_currency";
            case "sale_price", "saleprice" -> "sale_price";
            case "cost" -> "cost";
            case "status" -> "status";
            case "createdat", "created_at" -> "created_at";
            case "updatedat", "updated_at" -> "updated_at";
            default -> "name";
        };
    }
}
