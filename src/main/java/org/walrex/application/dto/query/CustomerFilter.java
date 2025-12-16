package org.walrex.application.dto.query;

import lombok.Builder;
import lombok.Data;

/**
 * Objeto de filtrado para consultas de Customer.
 *
 * Siguiendo el patrón hexagonal, este DTO pertenece a la capa de aplicación
 * y es independiente de la implementación de persistencia.
 */
@Data
@Builder
public class CustomerFilter {

    /**
     * Búsqueda general (busca en nombre, apellido, email o número de documento).
     */
    private String search;

    /**
     * Filtro exacto por tipo de documento.
     */
    private Integer idTypeDocument;

    /**
     * Filtro exacto por número de documento.
     */
    private String numberDocument;

    /**
     * Filtro por email.
     */
    private String email;

    /**
     * Filtro por género.
     */
    private String gender;

    /**
     * Filtro por país de residencia.
     */
    private Integer idCountryResidence;

    /**
     * Filtro por si es PEP (Persona Expuesta Políticamente).
     */
    private String isPEP;

    /**
     * Incluir registros eliminados (soft deleted).
     * Por defecto 1 (true).
     */
    @Builder.Default
    private String includeDeleted = "1";
}
