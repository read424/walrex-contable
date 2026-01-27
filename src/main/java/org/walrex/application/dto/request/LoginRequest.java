package org.walrex.application.dto.request;

import io.smallrye.common.constraint.NotNull;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "El nombre de usuario no puede estar vacío")
        String username,
        @NotBlank(message = "El pinHash no puede estar vacío")
        String pinHash
) {}
