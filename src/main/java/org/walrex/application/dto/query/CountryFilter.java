package org.walrex.application.dto.query;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CountryFilter {
    /**
     * Búsqueda general (busca en nombre o código alfabético).
     */
    private String search;

    /**
     * Filtro exacto por código alfabético ISO 4217 (ej: PE).
     */
    private String alphabeticCode2;

    /**
     * Filtro exacto por código alfabético ISO 4217 (ej: PER).
     */
    private String alphabeticCode3;

    /**
     * Filtro exacto por código numérico ISO 4217 (ej: 840).
     */
    private Integer numericCode;

    /**
     * Incluir registros eliminados (soft deleted).
     * Por defecto 1 .
     */
    @Builder.Default
    private String includeDeleted = "1";
}
