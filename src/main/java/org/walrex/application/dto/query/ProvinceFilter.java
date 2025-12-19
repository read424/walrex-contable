package org.walrex.application.dto.query;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class ProvinceFilter {

    /**
     * Filtro exacto por id departamento (ej: 1).
     */
    private Integer idDepartament;

    /**
     * Filtro exacto por codigo (ej: 010100).
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
