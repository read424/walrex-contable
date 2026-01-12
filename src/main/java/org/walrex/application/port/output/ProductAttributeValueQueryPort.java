package org.walrex.application.port.output;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.walrex.application.dto.query.PageRequest;
import org.walrex.application.dto.query.ProductAttributeValueFilter;
import org.walrex.domain.model.PagedResult;
import org.walrex.domain.model.ProductAttributeValue;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida (Output Port) para operaciones de lectura de valores de atributos de producto.
 *
 * Define las operaciones de consulta que el dominio necesita:
 * - findById: Buscar por ID
 * - findAll: Buscar con paginación y filtros
 * - findAllWithFilter: Buscar sin paginación con filtros
 * - count: Contar registros
 * - findByName: Buscar por nombre
 * - findByAttributeId: Buscar valores de un atributo específico
 * - existsById: Verificar existencia por ID
 * - existsByAttributeIdAndName: Verificar unicidad de (attributeId, name)
 *
 * Patrón Hexagonal:
 * Este puerto es DEFINIDO en la capa de aplicación pero IMPLEMENTADO
 * en la capa de infraestructura (por el Adapter de persistencia).
 *
 * IMPORTANTE: Todos los parámetros ID son de tipo Integer.
 */
public interface ProductAttributeValueQueryPort {

    /**
     * Busca un valor de atributo de producto por su ID.
     *
     * @param id ID del valor de atributo a buscar (Integer)
     * @return Uni con Optional del valor de atributo (vacío si no existe o está eliminado)
     */
    Uni<Optional<ProductAttributeValue>> findById(Integer id);

    /**
     * Lista valores de atributos con paginación, ordenamiento y filtros.
     *
     * @param pageRequest Configuración de paginación y ordenamiento
     * @param filter Filtros opcionales (search, attributeId, active, etc.)
     * @return Uni con resultado paginado
     */
    Uni<PagedResult<ProductAttributeValue>> findAll(PageRequest pageRequest, ProductAttributeValueFilter filter);

    /**
     * Lista todos los valores de atributos que cumplen el filtro (sin paginación).
     *
     * @param filter Filtros opcionales
     * @return Uni con lista completa de valores de atributos
     */
    Uni<List<ProductAttributeValue>> findAllWithFilter(ProductAttributeValueFilter filter);

    /**
     * Cuenta el total de valores de atributos que cumplen los filtros.
     *
     * @param filter Filtros opcionales
     * @return Uni con el conteo total
     */
    Uni<Long> count(ProductAttributeValueFilter filter);

    /**
     * Busca un valor de atributo por su nombre exacto.
     *
     * @param name Nombre del valor de atributo (case-insensitive)
     * @return Uni con Optional del valor de atributo (vacío si no existe)
     */
    Uni<Optional<ProductAttributeValue>> findByName(String name);

    /**
     * Busca todos los valores de un atributo específico.
     *
     * @param attributeId ID del atributo (Integer)
     * @return Uni con lista de valores del atributo
     */
    Uni<List<ProductAttributeValue>> findByAttributeId(Integer attributeId);

    /**
     * Verifica si existe un valor de atributo con el ID especificado.
     *
     * @param id ID a verificar (Integer)
     * @param excludeId ID a excluir de la verificación (Integer, para updates)
     * @return Uni<Boolean> true si existe, false si no
     */
    Uni<Boolean> existsById(Integer id, Integer excludeId);

    /**
     * Verifica si existe un valor de atributo con la combinación (attributeId, name).
     * La combinación debe ser única según constraint uk_attribute_value.
     *
     * @param attributeId ID del atributo (Integer)
     * @param name Nombre del valor
     * @param excludeId ID a excluir de la verificación (Integer, para updates)
     * @return Uni<Boolean> true si existe, false si no
     */
    Uni<Boolean> existsByAttributeIdAndName(Integer attributeId, String name, Integer excludeId);

    /**
     * Stream reactivo de todos los valores de atributos activos.
     *
     * @return Multi que emite cada valor de atributo individualmente
     */
    Multi<ProductAttributeValue> streamAll();

    /**
     * Stream reactivo de valores de atributos con filtros aplicados.
     *
     * @param filter Filtros a aplicar
     * @return Multi que emite cada valor de atributo que cumple los filtros
     */
    Multi<ProductAttributeValue> streamWithFilter(ProductAttributeValueFilter filter);
}
