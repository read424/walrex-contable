package org.walrex.domain.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.walrex.application.port.input.ConsultarPaisesInputPort;
import org.walrex.application.port.output.ChatOutputPort;
import org.walrex.domain.model.ChatMessage;
import org.walrex.domain.model.ChatResponse;
import org.walrex.domain.model.Intent;

import java.util.List;

/**
 * Orquestador principal del flujo de chat
 * Coordina: detección de intent -> ejecución de tool -> generación de respuesta con RAG
 */
@Slf4j
@ApplicationScoped
public class ChatOrchestrator {

    @Inject
    IntentMatcher intentMatcher;

    @Inject
    ChatOutputPort chatAdapter;

    @Inject
    ConsultarPaisesInputPort consultarPaisesUseCase;

    /**
     * Procesa un mensaje del usuario y genera una respuesta inteligente
     *
     * Flujo:
     * 1. Detecta intent usando embeddings + búsqueda semántica
     * 2. Ejecuta el tool MCP correspondiente
     * 3. Construye prompt RAG con los datos
     * 4. Genera respuesta amigable con el LLM
     *
     * @param message Mensaje del usuario
     * @return ChatResponse con la respuesta generada
     */
    public Uni<ChatResponse> processMessage(ChatMessage message) {
        log.info("Processing message from user: {}", message.userId());

        return intentMatcher.detectIntent(message.message())
                .onItem().transformToUni(intent -> {
                    if (intent == null) {
                        // No se detectó intent, respuesta genérica
                        log.warn("No intent detected, using fallback response");
                        return generateFallbackResponse(message.message());
                    }

                    log.info("Intent detected: {}, executing tool: {}",
                            intent.intentName(), intent.toolName());

                    // Ejecutar el tool correspondiente
                    return executeToolAndGenerateResponse(intent, message.message());
                });
    }

    /**
     * Ejecuta el tool asociado al intent y genera la respuesta RAG
     */
    private Uni<ChatResponse> executeToolAndGenerateResponse(Intent intent, String userMessage) {
        // Ejecutar el tool y obtener los datos
        Uni<String> toolData = executeTool(intent.toolName());

        return toolData.onItem().transformToUni(data -> {
            // Construir el prompt RAG usando el template del intent
            String prompt = buildRagPrompt(intent.promptTemplate(), userMessage, data);

            // Generar respuesta con el LLM
            return chatAdapter.generateResponse(prompt)
                    .map(response -> new ChatResponse(
                            response,
                            intent.intentName(),
                            intent.similarityScore(),
                            intent.toolName()
                    ));
        });
    }

    /**
     * Ejecuta un tool MCP por su nombre
     * TODO: Implementar dispatcher dinámico cuando haya más tools
     */
    private Uni<String> executeTool(String toolName) {
        // Si no hay tool configurado (ej: SALUDO), no ejecutar nada
        if (toolName == null || toolName.isBlank()) {
            log.debug("No tool configured for this intent, skipping tool execution");
            return Uni.createFrom().item("");
        }

        return switch (toolName) {
            case "consultarPaisesDisponibles" -> {
                List<String> paises = consultarPaisesUseCase.ejecutar();
                // Convertir la lista a texto formateado
                String resultado = String.join(", ", paises);
                log.info("Tool executed: {} returned {} countries", toolName, paises.size());
                yield Uni.createFrom().item(resultado);
            }
            default -> {
                log.warn("Unknown tool: {}", toolName);
                yield Uni.createFrom().item("No se pudo obtener la información solicitada.");
            }
        };
    }

    /**
     * Construye el prompt RAG reemplazando placeholders en el template
     *
     * Placeholders soportados:
     * - {question}: pregunta del usuario
     * - {data}: datos obtenidos del tool
     */
    private String buildRagPrompt(String template, String question, String data) {
        if (template == null || template.isBlank()) {
            // Template por defecto si no está configurado
            template = """
                Eres un asistente virtual amigable de una empresa de remesas.

                El cliente preguntó: {question}

                Información disponible: {data}

                Responde de manera clara, amigable y profesional. Menciona los países disponibles de forma natural.
                """;
        }

        String prompt = template
                .replace("{question}", question)
                .replace("{data}", data);

        log.debug("Built RAG prompt: {}", prompt.substring(0, Math.min(200, prompt.length())));
        return prompt;
    }

    /**
     * Genera respuesta de fallback cuando no se detecta ningún intent
     */
    private Uni<ChatResponse> generateFallbackResponse(String userMessage) {
        String systemPrompt = """
                Eres un asistente virtual de una empresa de remesas.
                El cliente te hizo una pregunta pero no pudiste identificar exactamente qué necesita.
                Responde de manera amigable y pide más detalles o sugiere temas que puedes ayudar (consulta de países disponibles, tasas de cambio, etc.).
                """;

        return chatAdapter.generateResponse(systemPrompt, userMessage)
                .map(response -> new ChatResponse(
                        response,
                        "UNKNOWN",
                        0.0,
                        null
                ));
    }
}
