package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.application.dto.response.AvailabilityResponse;

import java.util.List;

/**
 * Caso de uso para verificar disponibilidad de campos únicos.
 *
 * Útil para validaciones en tiempo real desde el frontend.
 */
public interface CheckAvailabilitySunatDocumentTypeUseCase {
    /**
     * Verifica si un ID está disponible.
     *
     * @param id ID a verificar
     * @param excludeId ID a excluir de la verificación (útil para updates)
     * @return AvailabilityResponse con el resultado
     */
    Uni<AvailabilityResponse> checkId(Integer id, String excludeId);

    /**
     * Verifica si un código SUNAT está disponible.
     *
     * @param code Código a verificar
     * @param excludeId ID a excluir de la verificación
     * @return AvailabilityResponse con el resultado
     */
    Uni<AvailabilityResponse> checkCode(String code, String excludeId);

    /**
     * Verifica disponibilidad de múltiples campos a la vez.
     *
     * @param id ID a verificar
     * @param code Código a verificar
     * @param excludeId ID a excluir de la verificación
     * @return Lista de AvailabilityResponse para cada campo verificado
     */
    Uni<List<AvailabilityResponse>> checkAll(Integer id, String code, String excludeId);
}
