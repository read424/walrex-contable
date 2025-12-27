package org.walrex.application.dto.response;

import java.time.LocalDateTime;

/**
 * DTO para respuesta de chat
 */
public record ChatResponseDto(
        String message,
        String detectedIntent,
        Double confidenceScore,
        String toolExecuted,
        LocalDateTime timestamp
) {}
