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
        log.info("Starting executeToolAndGenerateResponse - Intent: {}, Tool: {}, Template present: {}",
                intent.intentName(), intent.toolName(), intent.promptTemplate() != null);
        log.debug("Template content: {}", intent.promptTemplate());
        log.debug("User message: {}", userMessage);

        // Ejecutar el tool y obtener los datos
        Uni<String> toolData = executeTool(intent.toolName());

        return toolData.onItem().transformToUni(data -> {
            log.info("Tool data received, length: {}", data != null ? data.length() : 0);
            log.debug("Tool data content: {}", data);

            // Construir el prompt RAG usando el template del intent
            String prompt = buildRagPrompt(intent.promptTemplate(), userMessage, data);

            log.info("RAG prompt built successfully, sending to LLM");

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
     * Ejecuta un tool MCP por su nombre de forma reactiva
     * TODO: Implementar dispatcher dinámico cuando haya más tools
     */
    private Uni<String> executeTool(String toolName) {
        // Si no hay tool configurado (ej: SALUDO), no ejecutar nada
        if (toolName == null || toolName.isBlank()) {
            log.debug("No tool configured for this intent, skipping tool execution");
            return Uni.createFrom().item("");
        }

        return switch (toolName) {
            case "consultarPaisesDisponibles" -> consultarPaisesUseCase.consultarPaisesDisponibles()
                    .map(paisesDto -> {
                        // Formatear como: "Perú (Soles:Bolivares, Dollar:Bolivares), Venezuela (Bolivares:Soles)"
                        String resultado = paisesDto.stream()
                                .map(paisDto -> {
                                    // Extraer pares de monedas de cada ruta
                                    String rutasPares = paisDto.rutasDisponibles().stream()
                                            .map(this::extraerParMonedas)
                                            .collect(java.util.stream.Collectors.joining(", "));
                                    return paisDto.nombrePais() + " (" + rutasPares + ")";
                                })
                                .collect(java.util.stream.Collectors.joining(", "));

                        log.info("Tool executed: {} returned {} countries", toolName, paisesDto.size());
                        return resultado;
                    });
            default -> {
                log.warn("Unknown tool: {}", toolName);
                yield Uni.createFrom().item("No se pudo obtener la información solicitada.");
            }
        };
    }

    /**
     * Extrae el par de monedas de una ruta formateada
     * Entrada: "Soles → Bolívares (Venezuela)"
     * Salida: "Soles:Bolivares"
     */
    private String extraerParMonedas(String rutaFormateada) {
        // Dividir por → y extraer las monedas
        String[] partes = rutaFormateada.split(" → ");
        if (partes.length < 2) {
            return rutaFormateada; // Fallback si el formato no es el esperado
        }

        String monedaOrigen = partes[0].trim();
        // Extraer moneda destino eliminando el país entre paréntesis
        String monedaDestino = partes[1].split(" \\(")[0].trim();

        return monedaOrigen + ":" + monedaDestino;
    }

    /**
     * Construye el prompt RAG reemplazando placeholders en el template
     *
     * Placeholders soportados:
     * - {question}: pregunta del usuario
     * - {data}: datos obtenidos del tool
     */
    private String buildRagPrompt(String template, String question, String data) {
        log.info("Building RAG prompt - Template is null/blank: {}", template == null || template.isBlank());

        if (template == null || template.isBlank()) {
            log.warn("Using default template because intent template is null or blank");
            // Template por defecto si no está configurado
            template = """
                Eres un asistente virtual amigable de una empresa de remesas.

                El cliente preguntó: {question}

                Información disponible: {data}

                Responde de manera clara, amigable y profesional. Menciona los países disponibles de forma natural.
                """;
        } else {
            log.info("Using intent template from database");
            log.debug("Template before replacement: {}", template);
        }

        String prompt = template
                .replace("{question}", question)
                .replace("{data}", data);

        log.info("RAG prompt built successfully - Final length: {}", prompt.length());
        log.debug("Final prompt preview (first 300 chars): {}", prompt.substring(0, Math.min(300, prompt.length())));
        log.debug("Question placeholder replaced with: {}", question);
        log.debug("Data placeholder replaced with: {}", data != null ? data.substring(0, Math.min(100, data.length())) + "..." : "null");

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
