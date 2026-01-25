package org.walrex.domain.model;

import java.time.Instant;

public record RegistrationToken(
        String token,
        Instant expiresAt
) {}
