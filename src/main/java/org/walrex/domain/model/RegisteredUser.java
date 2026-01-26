package org.walrex.domain.model;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class RegisteredUser {

    private Integer userId;

    private Integer clientId;

    private String username;

    private OffsetDateTime createdAt;
}
