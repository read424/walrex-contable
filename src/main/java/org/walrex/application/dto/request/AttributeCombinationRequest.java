package org.walrex.application.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * DTO que representa un valor de atributo en una combinación.
 *
 * Ejemplo: Para una variante "Camiseta Roja L":
 * - attributeId: 10 (Color)
 * - valueId: 101 (Rojo)
 */
public record AttributeCombinationRequest(
    /**
     * ID del atributo (ej: Color, Talla)
     */
    @NotNull(message = "El ID del atributo es requerido")
    Integer attributeId,

    /**
     * ID del valor del atributo (ej: Rojo, L)
     */
    @NotNull(message = "El ID del valor del atributo es requerido")
    Integer valueId
) {
}
