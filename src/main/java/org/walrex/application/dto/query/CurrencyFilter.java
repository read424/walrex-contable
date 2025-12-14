package org.walrex.application.dto.query;

import lombok.Builder;
import lombok.Data;

/**
 * Objeto de filtrado para consultas de Currency.
 *
 * Siguiendo el patrón hexagonal, este DTO pertenece a la capa de aplicación
 * y es independiente de la implementación de persistencia.
 */
@Data
@Builder
public class CurrencyFilter {

    /**
     * Búsqueda general (busca en nombre o código alfabético).
     */
    private String search;

    /**
     * Filtro exacto por código alfabético ISO 4217 (ej: USD).
     */
    private String alphabeticCode;

    /**
     * Filtro exacto por código numérico ISO 4217 (ej: 840).
     */
    private String numericCode;

    /**
     * Filtro por estado activo/inactivo.
     */
    private String status;

    /**
     * Incluir registros eliminados (soft deleted).
     * Por defecto 1 (true).
     */
    @Builder.Default
    private String includeDeleted = "1";
}
