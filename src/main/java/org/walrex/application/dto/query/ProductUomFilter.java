package org.walrex.application.dto.query;

import lombok.Builder;
import lombok.Data;

/**
 * Objeto de filtrado para consultas de ProductUom.
 *
 * Siguiendo el patrón hexagonal, este DTO pertenece a la capa de aplicación
 * y es independiente de la implementación de persistencia.
 */
@Data
@Builder
public class ProductUomFilter {

    /**
     * Búsqueda general (busca en codeUom o nameUom).
     */
    private String search;

    /**
     * Filtro exacto por código de unidad de medida.
     */
    private String codeUom;

    /**
     * Filtro exacto por nombre de unidad de medida.
     */
    private String nameUom;

    /**
     * Filtro por ID de categoría.
     * Permite filtrar unidades de medida de una categoría específica.
     */
    private Integer categoryId;

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
