package org.walrex.application.dto.response;

/**
 * DTO de respuesta optimizado para componentes de selección (select, dropdown, autocomplete).
 * Contiene solo los campos esenciales necesarios para renderizar opciones en la UI.
 *
 * Nota: Usamos record (inmutable) para DTOs.
 */
public record AccountingAccountSelectResponse(
        Integer id,
        String code,
        String name
) {
}
