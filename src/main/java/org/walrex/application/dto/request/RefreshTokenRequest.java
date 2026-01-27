package org.walrex.application.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
        @NotBlank(message = "El refresh_token no puede estar vacío")
        @JsonProperty("refresh_token")
        String refreshToken
) {}
