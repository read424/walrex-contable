package org.walrex.application.dto.response;

import lombok.Builder;

import java.time.Instant;

@Builder
public record OtpValidationResponse(
        boolean valid,
        String target,
        String registrationToken,
        Instant tokenExpiresAt
) {}
