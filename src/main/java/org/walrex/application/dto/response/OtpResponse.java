package org.walrex.application.dto.response;

import java.time.Instant;

public record OtpResponse(
        String referenceId,
        Instant expiresAt
) {}