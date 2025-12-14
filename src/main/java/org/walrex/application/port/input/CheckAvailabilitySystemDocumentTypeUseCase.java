package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.application.dto.response.AvailabilityResponse;

import java.util.List;

/**
 * Puerto de entrada para verificar disponibilidad de campos únicos.
 */
public interface CheckAvailabilitySystemDocumentTypeUseCase {
    Uni<AvailabilityResponse> checkCode(String code, Long excludeId);

    Uni<AvailabilityResponse> checkName(String name, Long excludeId);

    Uni<List<AvailabilityResponse>> checkAll(String code, String name, Long excludeId);
}
