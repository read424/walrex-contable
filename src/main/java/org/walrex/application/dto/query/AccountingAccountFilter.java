package org.walrex.application.dto.query;

import lombok.Builder;
import lombok.Data;

/**
 * Objeto de filtrado para consultas de AccountingAccount.
 *
 * Siguiendo el patrón hexagonal, este DTO pertenece a la capa de aplicación
 * y es independiente de la implementación de persistencia.
 */
@Data
@Builder
public class AccountingAccountFilter {

    /**
     * Búsqueda general (busca en nombre o código).
     */
    private String search;

    /**
     * Filtro exacto por código de cuenta.
     */
    private String code;

    /**
     * Filtro exacto por nombre de cuenta.
     */
    private String name;

    /**
     * Filtro por tipo de cuenta.
     * Valores: ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE
     */
    private String type;

    /**
     * Filtro por lado normal.
     * Valores: DEBIT, CREDIT
     */
    private String normalSide;

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
