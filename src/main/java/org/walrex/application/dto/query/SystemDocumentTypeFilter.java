package org.walrex.application.dto.query;

import lombok.Builder;
import lombok.Data;

/**
 * Filtros para búsqueda de tipos de documento del sistema.
 *
 * Usa el patrón Builder para construcción flexible.
 */
@Data
@Builder
public class SystemDocumentTypeFilter {

    /**
     * Búsqueda general en código, nombre y descripción.
     */
    private String search;

    /**
     * Incluir registros eliminados (soft delete).
     * "1" = incluir eliminados, null o cualquier otro valor = solo activos
     */
    private String includeDeleted;

    /**
     * Filtro por código exacto.
     */
    private String code;

    /**
     * Filtro por si es requerido.
     */
    private Boolean isRequired;

    /**
     * Filtro por si aplica para personas.
     */
    private Boolean forPerson;

    /**
     * Filtro por si aplica para empresas.
     */
    private Boolean forCompany;

    /**
     * Filtro por estado activo.
     */
    private Boolean active;
}
