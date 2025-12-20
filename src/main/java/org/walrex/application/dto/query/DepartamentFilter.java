package org.walrex.application.dto.query;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DepartamentFilter {

    /**
     * Filtro exacto por codigo (ej: 010000).
     */
    private String codigo;

    /**
     * Filtro exacto por nombre (ej: LIMA).
     */
    private String name;

    /**
     * Incluir registros eliminados (soft deleted).
     * Por defecto false .
     */
    @Builder.Default
    private Boolean includeDeleted = true;
}
