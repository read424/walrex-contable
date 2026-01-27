package org.walrex.domain.model;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class RefreshToken {
    private Integer id;
    private Integer userId;
    private Integer deviceId; // Puede ser nulo si no se maneja
    private String refreshTokenHash;
    private OffsetDateTime expiresAt;
    private OffsetDateTime revokedAt;
    private OffsetDateTime createdAt;
}
