package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.application.dto.request.OtpGenerateRequest;
import org.walrex.application.dto.request.OtpValidateRequest;
import org.walrex.application.dto.response.OtpResponse;
import org.walrex.application.dto.response.OtpValidationResponse;

public interface OtpUseCase {
    Uni<OtpResponse> generateOtp(OtpGenerateRequest request);

    Uni<OtpValidationResponse> validateOtp(OtpValidateRequest request);
}
