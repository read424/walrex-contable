package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.application.dto.request.CalculateRateRequest;
import org.walrex.application.dto.response.RateCalculationResponse;

public interface CalculateRateTypeUseCase {
    Uni<RateCalculationResponse> calculateRate(CalculateRateRequest request);
}