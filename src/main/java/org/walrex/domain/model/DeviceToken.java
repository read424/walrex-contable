package org.walrex.domain.model;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class DeviceToken {
    private Integer id;
    private Integer userId;
    private String token;
    private PlatformType platform;
    private Boolean active;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
