package org.walrex.application.dto.response;

/**
 * DTO de respuesta optimizado para componentes de selección (select, dropdown, autocomplete).
 * Contiene solo los campos esenciales para tipos de comprobantes SUNAT.
 *
 * Nota: Usamos record (inmutable) para DTOs.
 */
public record TypeComprobantSunatSelectResponse(
        Integer id,
        String sunatCode,
        String nameDocument
) {
}
