package org.walrex.application.dto.response;

/**
 * DTO para respuesta de generación de embeddings
 */
public record EmbeddingGenerationResponse(
        int intentsProcessed,
        String message,
        long durationMs
) {}
