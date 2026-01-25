package org.walrex.domain.factory;

import org.walrex.domain.model.Otp;
import org.walrex.domain.model.OtpPurpose;

import java.time.Instant;
import java.util.UUID;

public final class OtpFactory {

    private OtpFactory() {}

    public static Otp create(
            String target,
            String otpHash,
            OtpPurpose purpose
    ) {

        Instant expiresAt = switch (purpose) {
            case REGISTER -> Instant.now().plusSeconds(300);
            case LOGIN -> Instant.now().plusSeconds(180);
            case PASSWORD_RESET -> Instant.now().plusSeconds(600);
            case MFA_SETUP -> Instant.now().plusSeconds(900);
        };

        return Otp.builder()
                .referenceId(UUID.randomUUID().toString())
                .purpose(purpose)
                .target(target)
                .otpHash(otpHash)
                .expiresAt(expiresAt)
                .used(false)
                .build();
    }
}
