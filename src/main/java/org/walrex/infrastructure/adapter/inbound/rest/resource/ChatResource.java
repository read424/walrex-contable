package org.walrex.infrastructure.adapter.inbound.rest.resource;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.walrex.application.dto.request.ChatRequest;
import org.walrex.application.dto.response.ChatResponseDto;
import org.walrex.application.dto.response.ErrorResponse;
import org.walrex.domain.model.ChatMessage;
import org.walrex.domain.service.ChatOrchestrator;

import java.util.UUID;

/**
 * REST endpoint para el chat de atención al cliente
 */
@Slf4j
@Path("/api/v1/chat")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Tag(name = "Chat", description = "Chat de atención al cliente con IA")
public class ChatResource {

    @Inject
    ChatOrchestrator chatOrchestrator;

    @POST
    @Path("/message")
    @Operation(
            summary = "Enviar mensaje al chat",
            description = "Procesa un mensaje del cliente y genera una respuesta inteligente usando IA"
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "Respuesta generada exitosamente",
                    content = @Content(schema = @Schema(implementation = ChatResponseDto.class))
            ),
            @APIResponse(
                    responseCode = "400",
                    description = "Request inválido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @APIResponse(
                    responseCode = "500",
                    description = "Error interno del servidor",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public Uni<Response> sendMessage(@Valid ChatRequest request) {
        log.info("Received chat message from user: {}, session: {}",
                request.userId(), request.sessionId());

        // Generar sessionId si no viene
        String sessionId = request.sessionId() != null && !request.sessionId().isBlank()
                ? request.sessionId()
                : UUID.randomUUID().toString();

        // Generar userId si no viene (para demo)
        String userId = request.userId() != null && !request.userId().isBlank()
                ? request.userId()
                : "anonymous";

        ChatMessage message = new ChatMessage(sessionId, request.message(), userId);

        return chatOrchestrator.processMessage(message)
                .map(response -> {
                    ChatResponseDto dto = new ChatResponseDto(
                            response.message(),
                            response.detectedIntent(),
                            response.confidenceScore(),
                            response.toolExecuted(),
                            response.timestamp()
                    );
                    return Response.ok(dto).build();
                })
                .onFailure().recoverWithItem(error -> {
                    log.error("Error processing chat message", error);
                    ErrorResponse errorResponse = new ErrorResponse(
                            500,
                            "Internal Server Error",
                            "Error al procesar el mensaje: " + error.getMessage()
                    );
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity(errorResponse)
                            .build();
                });
    }

    @GET
    @Path("/health")
    @Operation(summary = "Health check del chat", description = "Verifica que el servicio de chat está disponible")
    @APIResponse(responseCode = "200", description = "Servicio disponible")
    public Uni<Response> health() {
        return Uni.createFrom().item(
                Response.ok()
                        .entity(new HealthResponse("Chat service is running", "OK"))
                        .build()
        );
    }

    record HealthResponse(String message, String status) {}
}
