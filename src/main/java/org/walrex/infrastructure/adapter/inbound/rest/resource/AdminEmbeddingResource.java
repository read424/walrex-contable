package org.walrex.infrastructure.adapter.inbound.rest.resource;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
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
import org.walrex.application.dto.response.EmbeddingGenerationResponse;
import org.walrex.application.dto.response.ErrorResponse;
import org.walrex.application.port.input.GenerateIntentEmbeddingsUseCase;

/**
 * Endpoint administrativo para gestión de embeddings de intents
 */
@Slf4j
@Path("/api/v1/admin/embeddings")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Tag(name = "Admin - Embeddings", description = "Administración de embeddings de intents")
public class AdminEmbeddingResource {

    @Inject
    GenerateIntentEmbeddingsUseCase embeddingGenerator;

    @Inject
    org.walrex.application.port.output.IntentEmbeddingOutputPort intentPersistence;

    @Inject
    org.walrex.infrastructure.adapter.outbound.logging.EmbeddingDebugLogger debugLogger;

    @POST
    @Path("/generate")
    @Operation(
            summary = "Generar embeddings faltantes",
            description = "Genera embeddings para todos los intents que no los tienen"
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "Embeddings generados exitosamente",
                    content = @Content(schema = @Schema(implementation = EmbeddingGenerationResponse.class))
            ),
            @APIResponse(
                    responseCode = "500",
                    description = "Error al generar embeddings",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public Uni<Response> generateMissingEmbeddings() {
        log.info("Admin request: Generate missing embeddings");
        long startTime = System.currentTimeMillis();

        return embeddingGenerator.generateMissingEmbeddings()
                .map(count -> {
                    long duration = System.currentTimeMillis() - startTime;
                    EmbeddingGenerationResponse response = new EmbeddingGenerationResponse(
                            count,
                            String.format("Successfully generated %d embeddings", count),
                            duration
                    );
                    return Response.ok(response).build();
                })
                .onFailure().recoverWithItem(error -> {
                    log.error("Error generating embeddings", error);
                    ErrorResponse errorResponse = new ErrorResponse(
                            500,
                            "Internal Server Error",
                            "Error al generar embeddings: " + error.getMessage()
                    );
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity(errorResponse)
                            .build();
                });
    }

    @POST
    @Path("/regenerate")
    @Operation(
            summary = "Regenerar todos los embeddings",
            description = "Regenera embeddings para TODOS los intents (sobrescribe los existentes)"
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "Embeddings regenerados exitosamente",
                    content = @Content(schema = @Schema(implementation = EmbeddingGenerationResponse.class))
            ),
            @APIResponse(
                    responseCode = "500",
                    description = "Error al regenerar embeddings",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public Uni<Response> regenerateAllEmbeddings() {
        log.info("Admin request: Regenerate all embeddings");
        long startTime = System.currentTimeMillis();

        return embeddingGenerator.regenerateAllEmbeddings()
                .map(count -> {
                    long duration = System.currentTimeMillis() - startTime;
                    EmbeddingGenerationResponse response = new EmbeddingGenerationResponse(
                            count,
                            String.format("Successfully regenerated %d embeddings", count),
                            duration
                    );
                    return Response.ok(response).build();
                })
                .onFailure().recoverWithItem(error -> {
                    log.error("Error regenerating embeddings", error);
                    ErrorResponse errorResponse = new ErrorResponse(
                            500,
                            "Internal Server Error",
                            "Error al regenerar embeddings: " + error.getMessage()
                    );
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity(errorResponse)
                            .build();
                });
    }

    @POST
    @Path("/generate/{intentName}")
    @Operation(
            summary = "Generar embedding para un intent específico",
            description = "Genera/regenera el embedding para un intent por su nombre"
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "Embedding generado exitosamente"
            ),
            @APIResponse(
                    responseCode = "404",
                    description = "Intent no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @APIResponse(
                    responseCode = "500",
                    description = "Error al generar embedding",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public Uni<Response> generateEmbeddingForIntent(@PathParam("intentName") String intentName) {
        log.info("Admin request: Generate embedding for intent: {}", intentName);
        long startTime = System.currentTimeMillis();

        return embeddingGenerator.generateEmbeddingForIntent(intentName)
                .map(success -> {
                    long duration = System.currentTimeMillis() - startTime;
                    EmbeddingGenerationResponse response = new EmbeddingGenerationResponse(
                            1,
                            String.format("Successfully generated embedding for intent: %s", intentName),
                            duration
                    );
                    return Response.ok(response).build();
                })
                .onFailure(IllegalArgumentException.class).recoverWithItem(error -> {
                    ErrorResponse errorResponse = new ErrorResponse(
                            404,
                            "Not Found",
                            error.getMessage()
                    );
                    return Response.status(Response.Status.NOT_FOUND)
                            .entity(errorResponse)
                            .build();
                })
                .onFailure().recoverWithItem(error -> {
                    log.error("Error generating embedding for intent: {}", intentName, error);
                    ErrorResponse errorResponse = new ErrorResponse(
                            500,
                            "Internal Server Error",
                            "Error al generar embedding: " + error.getMessage()
                    );
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity(errorResponse)
                            .build();
                });
    }

}
