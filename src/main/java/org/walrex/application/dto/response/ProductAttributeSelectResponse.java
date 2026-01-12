package org.walrex.application.dto.response;

/**
 * DTO optimizado para componentes de selección (dropdowns, select).
 *
 * Contiene solo los campos esenciales para reducir el tamaño de la respuesta.
 */
public record ProductAttributeSelectResponse(
        Integer id,
        String name
) {
}
