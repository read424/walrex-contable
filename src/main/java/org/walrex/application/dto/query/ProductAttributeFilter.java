package org.walrex.application.dto.query;

import lombok.Builder;
import lombok.Data;
import org.walrex.domain.model.AttributeDisplayType;

/**
 * Filtros para consultas de atributos de producto.
 *
 * Todos los campos son opcionales. Si no se especifica un campo, no se aplica ese filtro.
 */
@Data
@Builder
public class ProductAttributeFilter {

    /**
     * Búsqueda general por ID o nombre (case-insensitive)
     * Busca coincidencias parciales en ambos campos
     */
    private String search;

    /**
     * Filtro por nombre exacto (case-insensitive)
     */
    private String name;

    /**
     * Filtro por tipo de visualización
     * Valores válidos: SELECT, RADIO, COLOR, TEXT
     */
    private AttributeDisplayType displayType;

    /**
     * Filtro por estado activo/inactivo
     * - "1" = solo activos
     * - "0" = solo inactivos
     * - null = todos
     */
    private String active;

    /**
     * Indica si se deben incluir registros eliminados
     * - "1" = incluir eliminados
     * - "0" = excluir eliminados (default)
     */
    @Builder.Default
    private String includeDeleted = "0";
}
