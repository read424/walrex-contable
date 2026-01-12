package org.walrex.application.port.output;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.walrex.application.dto.query.PageRequest;
import org.walrex.application.dto.query.ProductAttributeFilter;
import org.walrex.domain.model.PagedResult;
import org.walrex.domain.model.ProductAttribute;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida para consultas de atributos de producto.
 *
 * Siguiendo el patrón hexagonal, este puerto define las operaciones
 * de lectura sin especificar la tecnología (PostgreSQL, etc.).
 *
 * NOTA: Este puerto usa Integer como tipo de ID (auto-generado).
 */
public interface ProductAttributeQueryPort {

    // ==================== Búsquedas por ID ====================

    /**
     * Busca un atributo activo por su ID.
     *
     * SQL: SELECT * FROM product_attributes WHERE id = $1 AND deleted_at IS NULL
     *
     * @param id Identificador único (Integer)
     * @return Uni con Optional del atributo (vacío si no existe o está eliminado)
     */
    Uni<Optional<ProductAttribute>> findById(Integer id);

    /**
     * Busca un atributo por ID incluyendo eliminados.
     *
     * SQL: SELECT * FROM product_attributes WHERE id = $1
     *
     * @param id Identificador único (Integer)
     * @return Uni con Optional del atributo
     */
    Uni<Optional<ProductAttribute>> findByIdIncludingDeleted(Integer id);

    // ==================== Búsquedas por Nombre ====================

    /**
     * Busca un atributo por nombre (case-insensitive).
     *
     * SQL: SELECT * FROM product_attributes WHERE LOWER(name) = LOWER($1) AND deleted_at IS NULL
     *
     * @param name Nombre del atributo
     * @return Uni con Optional del atributo
     */
    Uni<Optional<ProductAttribute>> findByName(String name);

    // ==================== Verificaciones de Existencia ====================

    /**
     * Verifica si existe un atributo con el nombre.
     *
     * SQL: SELECT EXISTS(SELECT 1 FROM product_attributes WHERE LOWER(name) = LOWER($1)
     *      AND deleted_at IS NULL [AND id != $2])
     *
     * @param name Nombre a verificar
     * @param excludeId ID a excluir de la búsqueda (null para no excluir)
     * @return Uni<Boolean> true si existe
     */
    Uni<Boolean> existsByName(String name, Integer excludeId);

    // ==================== Listados con Paginación ====================

    /**
     * Lista atributos con paginación y filtros.
     *
     * SQL dinámico con:
     * - WHERE conditions basadas en ProductAttributeFilter
     * - ORDER BY basado en PageRequest.sortBy
     * - LIMIT $n OFFSET $m para paginación
     *
     * @param pageRequest Configuración de paginación
     * @param filter Filtros opcionales
     * @return Uni con resultado paginado incluyendo metadata
     */
    Uni<PagedResult<ProductAttribute>> findAll(PageRequest pageRequest, ProductAttributeFilter filter);

    /**
     * Cuenta el total de atributos que cumplen los filtros.
     *
     * SQL: SELECT COUNT(*) FROM product_attributes WHERE [conditions]
     *
     * @param filter Filtros opcionales
     * @return Uni con el conteo total
     */
    Uni<Long> count(ProductAttributeFilter filter);

    // ==================== Listados sin Paginación ====================

    /**
     * Lista todos los atributos que cumplen el filtro sin paginación.
     *
     * SQL dinámico con:
     * - WHERE conditions basadas en ProductAttributeFilter
     * - ORDER BY name ASC (por defecto)
     *
     * @param filter Filtros opcionales
     * @return Uni con lista completa de atributos
     */
    Uni<List<ProductAttribute>> findAllWithFilter(ProductAttributeFilter filter);

    // ==================== Streaming ====================

    /**
     * Obtiene todos los atributos activos como stream.
     *
     * Usa RowSet y lo convierte a Multi para procesamiento reactivo.
     *
     * @return Multi que emite cada atributo
     */
    Multi<ProductAttribute> streamAll();

    /**
     * Obtiene atributos como stream aplicando filtros.
     *
     * @param filter Filtros a aplicar
     * @return Multi que emite cada atributo que cumple los filtros
     */
    Multi<ProductAttribute> streamWithFilter(ProductAttributeFilter filter);

    // ==================== Consultas Especiales ====================

    /**
     * Lista todos los atributos eliminados (para posible restauración).
     *
     * SQL: SELECT * FROM product_attributes WHERE deleted_at IS NOT NULL
     *
     * @return Uni con lista de atributos eliminados
     */
    Uni<List<ProductAttribute>> findAllDeleted();
}
