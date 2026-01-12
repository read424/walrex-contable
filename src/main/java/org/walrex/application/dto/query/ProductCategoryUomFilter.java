package org.walrex.application.dto.query;

import lombok.Builder;
import lombok.Data;

/**
 * Objeto de filtrado para consultas de ProductCategoryUom.
 *
 * Siguiendo el patrón hexagonal, este DTO pertenece a la capa de aplicación
 * y es independiente de la implementación de persistencia.
 */
@Data
@Builder
public class ProductCategoryUomFilter {

    /**
     * Búsqueda general (busca en nombre o código).
     */
    private String search;

    /**
     * Filtro exacto por código de categoría.
     */
    private String code;

    /**
     * Filtro exacto por nombre de categoría.
     */
    private String name;

    /**
     * Filtro por estado activo/inactivo.
     * Valores: "1" (activos), "0" (inactivos), null (todos)
     */
    private String active;

    /**
     * Incluir registros eliminados (soft deleted).
     * Por defecto "0" (false) - solo mostrar registros no eliminados.
     */
    @Builder.Default
    private String includeDeleted = "0";
}
