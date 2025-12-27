package org.walrex.domain.model;

import java.time.LocalDateTime;

/**
 * Representa la respuesta del chat al usuario
 */
public record ChatResponse(
        String message,
        String detectedIntent,
        Double confidenceScore,
        String toolExecuted,
        LocalDateTime timestamp
) {
    public ChatResponse(String message, String detectedIntent, Double confidenceScore, String toolExecuted) {
        this(message, detectedIntent, confidenceScore, toolExecuted, LocalDateTime.now());
    }
}
