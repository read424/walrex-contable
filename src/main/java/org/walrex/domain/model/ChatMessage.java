package org.walrex.domain.model;

import java.time.LocalDateTime;

/**
 * Representa un mensaje en el chat
 */
public record ChatMessage(
        String sessionId,
        String message,
        String userId,
        LocalDateTime timestamp
) {
    public ChatMessage(String sessionId, String message, String userId) {
        this(sessionId, message, userId, LocalDateTime.now());
    }
}
