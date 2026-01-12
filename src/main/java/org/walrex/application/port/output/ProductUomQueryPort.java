package org.walrex.application.port.output;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.walrex.application.dto.query.PageRequest;
import org.walrex.application.dto.query.ProductUomFilter;
import org.walrex.domain.model.PagedResult;
import org.walrex.domain.model.ProductUom;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida para operaciones de consulta de ProductUom.
 *
 * Siguiendo el patrón hexagonal, este puerto define las operaciones
 * de lectura sin especificar la implementación.
 */
public interface ProductUomQueryPort {

    /**
     * Busca una unidad de medida por ID (solo activas, no eliminadas).
     *
     * @param id ID de la unidad
     * @return Uni con Optional que contiene la unidad si existe y no está eliminada
     */
    Uni<Optional<ProductUom>> findById(Integer id);

    /**
     * Busca una unidad de medida por ID incluyendo eliminadas.
     *
     * @param id ID de la unidad
     * @return Uni con Optional que contiene la unidad si existe (incluso si está eliminada)
     */
    Uni<Optional<ProductUom>> findByIdIncludingDeleted(Integer id);

    /**
     * Busca una unidad de medida por código (solo activas, no eliminadas).
     *
     * @param code Código de la unidad
     * @return Uni con Optional que contiene la unidad si existe
     */
    Uni<Optional<ProductUom>> findByCode(String code);

    /**
     * Busca una unidad de medida por nombre exacto (solo activas, no eliminadas).
     *
     * @param name Nombre de la unidad
     * @return Uni con Optional que contiene la unidad si existe
     */
    Uni<Optional<ProductUom>> findByName(String name);

    /**
     * Verifica si existe una unidad de medida con el código.
     * Útil para validaciones de unicidad.
     *
     * @param code Código a verificar
     * @param excludeId ID a excluir de la búsqueda (útil para updates), null para incluir todos
     * @return Uni<Boolean> true si existe
     */
    Uni<Boolean> existsByCode(String code, Integer excludeId);

    /**
     * Verifica si existe una unidad de medida con el nombre.
     *
     * @param name Nombre a verificar
     * @param excludeId ID a excluir de la búsqueda, null para incluir todos
     * @return Uni<Boolean> true si existe
     */
    Uni<Boolean> existsByName(String name, Integer excludeId);

    /**
     * Lista unidades de medida con paginación y filtros.
     *
     * @param pageRequest Configuración de paginación
     * @param filter Filtros opcionales
     * @return Uni con resultado paginado
     */
    Uni<PagedResult<ProductUom>> findAll(PageRequest pageRequest, ProductUomFilter filter);

    /**
     * Cuenta el total de unidades que cumplen los filtros.
     *
     * @param filter Filtros opcionales
     * @return Uni con el total de registros
     */
    Uni<Long> count(ProductUomFilter filter);

    /**
     * Obtiene todas las unidades de medida que cumplen el filtro sin paginación.
     * Útil para componentes de selección.
     *
     * @param filter Filtros opcionales
     * @return Uni con lista completa
     */
    Uni<List<ProductUom>> findAllWithFilter(ProductUomFilter filter);

    /**
     * Stream reactivo de todas las unidades activas (no eliminadas).
     *
     * @return Multi que emite cada unidad individualmente
     */
    Multi<ProductUom> streamAll();

    /**
     * Stream reactivo de unidades con filtros.
     *
     * @param filter Filtros a aplicar
     * @return Multi que emite cada unidad que cumple los filtros
     */
    Multi<ProductUom> streamWithFilter(ProductUomFilter filter);

    /**
     * Obtiene todas las unidades eliminadas (soft deleted).
     * Útil para operaciones de administración.
     *
     * @return Uni con lista de unidades eliminadas
     */
    Uni<List<ProductUom>> findAllDeleted();
}
