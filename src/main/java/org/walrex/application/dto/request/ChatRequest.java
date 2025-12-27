package org.walrex.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO para request de mensaje de chat
 */
public record ChatRequest(
        @NotBlank(message = "El mensaje no puede estar vacío")
        @Size(min = 1, max = 2000, message = "El mensaje debe tener entre 1 y 2000 caracteres")
        String message,

        String sessionId,
        String userId
) {}
