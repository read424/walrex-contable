package org.walrex.application.dto.query;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class DistrictFilter {
    /**
     * Filtro exacto por id Provincia (ej: 1).
     */
    private Integer idProvince;

    /**
     * Filtro exacto por codigo (ej: 010101).
     */
    private String codigo;

    /**
     * Filtro exacto por nombre (ej: Chachapoyas).
     */
    private String name;

    /**
     * Incluir registros eliminados (soft deleted).
     * Por defecto false .
     */
    @Builder.Default
    private Boolean includeDeleted = true;
}
