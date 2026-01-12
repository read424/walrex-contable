package org.walrex.application.dto.response;

/**
 * DTO de respuesta optimizado para componentes de selección (dropdowns, autocompletes).
 *
 * Contiene solo los campos esenciales:
 * - id: identificador único
 * - codeUom: código para mostrar
 * - nameUom: nombre descriptivo
 *
 * Este DTO reduce el tamaño de la respuesta para listas grandes.
 */
public record ProductUomSelectResponse(
        Integer id,
        String codeUom,
        String nameUom
) {
}
