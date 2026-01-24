package org.walrex.domain.model;

import lombok.*;
import org.walrex.domain.service.OtpHasher;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Otp {
    private String referenceId;
    private OtpPurpose purpose;
    private String target;
    private String otpHash;
    private Instant expiresAt;
    private boolean used;

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public void markAsUsed() {
        this.used = true;
    }

    public boolean matches(String rawCode, OtpHasher hasher) {
        return hasher.matches(rawCode, this.otpHash);
    }
}
