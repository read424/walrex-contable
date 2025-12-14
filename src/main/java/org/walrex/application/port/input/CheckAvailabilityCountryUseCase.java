package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.application.dto.response.AvailabilityResponse;

import java.util.List;

public interface CheckAvailabilityCountryUseCase {
    /**
     * Verifica si un código alfabético está disponible.
     *
     * @param alphabeticCode Código a verificar (3 letras)
     * @param excludeId ID a excluir de la verificación (para updates), puede ser null
     * @return Uni con el resultado de disponibilidad
     */
    Uni<AvailabilityResponse> checkAlphabeticCode(String alphabeticCode, Integer excludeId);

    /**
     * Verifica si un código numérico está disponible.
     *
     * @param numericCode Código a verificar (3 dígitos)
     * @param excludeId ID a excluir de la verificación (para updates), puede ser null
     * @return Uni con el resultado de disponibilidad
     */
    Uni<AvailabilityResponse> checkNumericCode(Integer numericCode, Integer excludeId);

    /**
     * Verifica si un nombre está disponible.
     *
     * @param name Nombre a verificar
     * @param excludeId ID a excluir de la verificación (para updates), puede ser null
     * @return Uni con el resultado de disponibilidad
     */
    Uni<AvailabilityResponse> checkName(String name, Integer excludeId);

    /**
     * Verifica disponibilidad de múltiples campos a la vez.
     *
     * Útil para validar todo el formulario de una sola vez.
     *
     * @param alphabeticCode Código alfabético (puede ser null)
     * @param numericCode Código numérico (puede ser null)
     * @param name Nombre (puede ser null)
     * @param excludeId ID a excluir (para updates)
     * @return Uni con lista de resultados de disponibilidad
     */
    Uni<List<AvailabilityResponse>> checkAll(
            String alphabeticCode,
            String numericCode,
            String name,
            Integer excludeId
    );
}
