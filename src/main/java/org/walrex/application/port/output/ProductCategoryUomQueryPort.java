package org.walrex.application.port.output;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.walrex.application.dto.query.PageRequest;
import org.walrex.application.dto.query.ProductCategoryUomFilter;
import org.walrex.domain.model.PagedResult;
import org.walrex.domain.model.ProductCategoryUom;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida para consultas de categorías de unidades de medida.
 *
 * Siguiendo el patrón hexagonal, este puerto define las operaciones
 * de lectura sin especificar la tecnología (PostgreSQL, etc.).
 */
public interface ProductCategoryUomQueryPort {

    // ==================== Búsquedas por ID ====================

    /**
     * Busca una categoría activa por su ID.
     *
     * SQL: SELECT * FROM product_category_uom WHERE id = $1 AND deleted_at IS NULL
     *
     * @param id Identificador único
     * @return Uni con Optional de la categoría (vacío si no existe o está eliminada)
     */
    Uni<Optional<ProductCategoryUom>> findById(Integer id);

    /**
     * Busca una categoría por ID incluyendo eliminadas.
     *
     * SQL: SELECT * FROM product_category_uom WHERE id = $1
     *
     * @param id Identificador único
     * @return Uni con Optional de la categoría
     */
    Uni<Optional<ProductCategoryUom>> findByIdIncludingDeleted(Integer id);

    // ==================== Búsquedas por Código ====================

    /**
     * Busca una categoría por código único.
     *
     * SQL: SELECT * FROM product_category_uom WHERE code = $1 AND deleted_at IS NULL
     *
     * @param code Código único de la categoría
     * @return Uni con Optional de la categoría
     */
    Uni<Optional<ProductCategoryUom>> findByCode(String code);

    /**
     * Busca una categoría por nombre (case-insensitive).
     *
     * SQL: SELECT * FROM product_category_uom WHERE LOWER(name) = LOWER($1) AND deleted_at IS NULL
     *
     * @param name Nombre de la categoría
     * @return Uni con Optional de la categoría
     */
    Uni<Optional<ProductCategoryUom>> findByName(String name);

    // ==================== Verificaciones de Existencia ====================

    /**
     * Verifica si existe una categoría con el código.
     *
     * SQL: SELECT EXISTS(SELECT 1 FROM product_category_uom WHERE code = $1
     *      AND deleted_at IS NULL [AND id != $2])
     *
     * @param code Código a verificar
     * @param excludeId ID a excluir de la búsqueda (null para no excluir)
     * @return Uni<Boolean> true si existe
     */
    Uni<Boolean> existsByCode(String code, Integer excludeId);

    /**
     * Verifica si existe una categoría con el nombre.
     *
     * SQL: SELECT EXISTS(SELECT 1 FROM product_category_uom WHERE LOWER(name) = LOWER($1)
     *      AND deleted_at IS NULL [AND id != $2])
     *
     * @param name Nombre a verificar
     * @param excludeId ID a excluir de la búsqueda (null para no excluir)
     * @return Uni<Boolean> true si existe
     */
    Uni<Boolean> existsByName(String name, Integer excludeId);

    // ==================== Listados con Paginación ====================

    /**
     * Lista categorías con paginación y filtros.
     *
     * SQL dinámico con:
     * - WHERE conditions basadas en ProductCategoryUomFilter
     * - ORDER BY basado en PageRequest.sortBy
     * - LIMIT $n OFFSET $m para paginación
     *
     * @param pageRequest Configuración de paginación
     * @param filter Filtros opcionales
     * @return Uni con resultado paginado incluyendo metadata
     */
    Uni<PagedResult<ProductCategoryUom>> findAll(PageRequest pageRequest, ProductCategoryUomFilter filter);

    /**
     * Cuenta el total de categorías que cumplen los filtros.
     *
     * SQL: SELECT COUNT(*) FROM product_category_uom WHERE [conditions]
     *
     * @param filter Filtros opcionales
     * @return Uni con el conteo total
     */
    Uni<Long> count(ProductCategoryUomFilter filter);

    // ==================== Listados sin Paginación ====================

    /**
     * Lista todas las categorías que cumplen el filtro sin paginación.
     *
     * SQL dinámico con:
     * - WHERE conditions basadas en ProductCategoryUomFilter
     * - ORDER BY name ASC (por defecto)
     *
     * @param filter Filtros opcionales
     * @return Uni con lista completa de categorías
     */
    Uni<List<ProductCategoryUom>> findAllWithFilter(ProductCategoryUomFilter filter);

    // ==================== Streaming ====================

    /**
     * Obtiene todas las categorías activas como stream.
     *
     * Usa RowSet y lo convierte a Multi para procesamiento reactivo.
     *
     * @return Multi que emite cada categoría
     */
    Multi<ProductCategoryUom> streamAll();

    /**
     * Obtiene categorías como stream aplicando filtros.
     *
     * @param filter Filtros a aplicar
     * @return Multi que emite cada categoría que cumple los filtros
     */
    Multi<ProductCategoryUom> streamWithFilter(ProductCategoryUomFilter filter);

    // ==================== Consultas Especiales ====================

    /**
     * Lista todas las categorías eliminadas (para posible restauración).
     *
     * SQL: SELECT * FROM product_category_uom WHERE deleted_at IS NOT NULL
     *
     * @return Uni con lista de categorías eliminadas
     */
    Uni<List<ProductCategoryUom>> findAllDeleted();
}
