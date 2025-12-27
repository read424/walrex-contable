package org.walrex.application.port.output;

import io.smallrye.mutiny.Uni;

/**
 * Puerto de salida para interacción con el modelo de chat
 */
public interface ChatOutputPort {
    /**
     * Genera una respuesta del modelo de chat basada en un prompt
     *
     * @param prompt Prompt completo para el modelo
     * @return Uni con la respuesta generada
     */
    Uni<String> generateResponse(String prompt);

    /**
     * Genera una respuesta del modelo usando un system prompt y mensaje del usuario
     *
     * @param systemPrompt Instrucciones del sistema para el modelo
     * @param userMessage Mensaje del usuario
     * @return Uni con la respuesta generada
     */
    Uni<String> generateResponse(String systemPrompt, String userMessage);
}
