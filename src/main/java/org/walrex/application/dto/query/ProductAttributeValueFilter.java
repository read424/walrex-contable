package org.walrex.application.dto.query;

import lombok.Builder;
import lombok.Data;

/**
 * Objeto de filtrado para consultas de valores de atributos de producto.
 *
 * Proporciona múltiples criterios de filtrado que se pueden combinar:
 * - Búsqueda general (search): busca en id o nombre
 * - Nombre exacto (name): busca por nombre exacto
 * - Atributo (attributeId): filtra valores de un atributo específico
 * - Estado activo (active): filtra por activos/inactivos
 * - Incluir eliminados (includeDeleted): incluye/excluye registros eliminados
 *
 * Todos los filtros son opcionales y se combinan con lógica AND.
 * Si no se proporciona ningún filtro, se retornan todos los valores activos.
 */
@Data
@Builder
public class ProductAttributeValueFilter {

    /**
     * Búsqueda general en id o nombre.
     * Si se proporciona, busca coincidencias parciales case-insensitive.
     */
    private String search;

    /**
     * Nombre exacto del valor de atributo.
     * Búsqueda case-insensitive.
     */
    private String name;

    /**
     * ID del atributo para filtrar valores de un atributo específico.
     * Útil para obtener todos los valores de un atributo (ej: todos los colores, todas las tallas).
     */
    private Integer attributeId;

    /**
     * Filtro por estado activo/inactivo.
     * - "1": solo activos
     * - "0": solo inactivos
     * - null/empty: todos
     */
    private String active;

    /**
     * Incluir registros eliminados (soft delete).
     * - "1": incluir eliminados
     * - "0" o null: excluir eliminados (comportamiento por defecto)
     */
    @Builder.Default
    private String includeDeleted = "0";
}
