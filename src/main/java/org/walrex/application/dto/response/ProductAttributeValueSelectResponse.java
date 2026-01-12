package org.walrex.application.dto.response;

/**
 * DTO de respuesta optimizado para componentes de selección (select, dropdown, etc).
 *
 * Solo incluye los campos esenciales:
 * - id: identificador único
 * - name: nombre descriptivo para mostrar
 * - htmlColor: color asociado (opcional, para visualización)
 *
 * Este DTO reduce el payload de la API para listados completos sin paginación,
 * especialmente útil para formularios que necesitan mostrar todos los valores
 * de un atributo en un selector.
 */
public record ProductAttributeValueSelectResponse(
        Integer id,
        String name,
        String htmlColor
) {
}
