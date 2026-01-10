package org.walrex.infrastructure.adapter.inbound.rest.resource;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
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
import org.walrex.application.port.input.SyncAccountEmbeddingsUseCase;
import org.walrex.application.port.input.SyncHistoricalEntriesUseCase;
import org.walrex.application.port.output.IntentEmbeddingOutputPort;
import org.walrex.infrastructure.adapter.outbound.logging.EmbeddingDebugLogger;

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
    IntentEmbeddingOutputPort intentPersistence;

    @Inject
    EmbeddingDebugLogger debugLogger;

    @Inject
    SyncHistoricalEntriesUseCase historicalEntriesSyncUseCase;

    @Inject
    SyncAccountEmbeddingsUseCase syncAccountEmbeddingsUseCase;

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

    // ==================== Accounting Accounts Sync ====================

    @POST
    @WithSession
    @Path("/sync-accounts")
    @Operation(
            summary = "Sincronizar cuentas contables a Qdrant",
            description = """
                    Sincroniza las cuentas contables que aún no han sido procesadas a Qdrant.

                    Genera embeddings del catálogo de cuentas (Chart of Accounts) para que el RAG
                    pueda sugerir cuentas contables apropiadas cuando se procesen documentos.

                    Solo procesa cuentas que no han sido sincronizadas previamente.
                    """
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "Cuentas sincronizadas exitosamente",
                    content = @Content(schema = @Schema(implementation = org.walrex.domain.model.SyncResult.class))
            ),
            @APIResponse(
                    responseCode = "500",
                    description = "Error al sincronizar cuentas",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public Uni<Response> syncUnsyncedAccounts() {
        log.info("Admin request: Sync unsynced accounting accounts to Qdrant");

        return syncAccountEmbeddingsUseCase.syncUnsyncedAccounts()
                .map(syncResult -> Response.ok(syncResult).build())
                .onFailure().recoverWithItem(error -> {
                    log.error("Error syncing accounting accounts", error);
                    ErrorResponse errorResponse = new ErrorResponse(
                            500,
                            "Internal Server Error",
                            "Error al sincronizar cuentas contables: " + error.getMessage()
                    );
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity(errorResponse)
                            .build();
                });
    }

    @POST
    @WithSession
    @Path("/sync-account/{accountId}")
    @Operation(
            summary = "Sincronizar una cuenta contable específica",
            description = "Sincroniza una cuenta contable específica a Qdrant por su ID"
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "Cuenta sincronizada exitosamente"
            ),
            @APIResponse(
                    responseCode = "404",
                    description = "Cuenta no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @APIResponse(
                    responseCode = "500",
                    description = "Error al sincronizar cuenta",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public Uni<Response> syncAccount(@PathParam("accountId") Integer accountId) {
        log.info("Admin request: Sync accounting account: {}", accountId);
        long startTime = System.currentTimeMillis();

        return syncAccountEmbeddingsUseCase.syncAccount(accountId)
                .map(success -> {
                    long duration = System.currentTimeMillis() - startTime;
                    EmbeddingGenerationResponse response = new EmbeddingGenerationResponse(
                            1,
                            String.format("Successfully synced accounting account %d to Qdrant", accountId),
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
                    log.error("Error syncing accounting account: {}", accountId, error);
                    ErrorResponse errorResponse = new ErrorResponse(
                            500,
                            "Internal Server Error",
                            "Error al sincronizar cuenta: " + error.getMessage()
                    );
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity(errorResponse)
                            .build();
                });
    }

    @POST
    @WithSession
    @Path("/resync-all-accounts")
    @Operation(
            summary = "Re-sincronizar todas las cuentas contables",
            description = """
                    Fuerza la re-sincronización de TODAS las cuentas contables activas a Qdrant.

                    Útil cuando:
                    - Se actualiza el modelo de embeddings
                    - Se modifican múltiples cuentas manualmente en la DB
                    - Se necesita regenerar todos los embeddings del catálogo de cuentas

                    ADVERTENCIA: Este proceso puede tardar si hay muchas cuentas.
                    """
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "Cuentas re-sincronizadas exitosamente",
                    content = @Content(schema = @Schema(implementation = org.walrex.domain.model.SyncResult.class))
            ),
            @APIResponse(
                    responseCode = "500",
                    description = "Error al re-sincronizar cuentas",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public Uni<Response> forceResyncAllAccounts() {
        log.info("Admin request: Force resync all accounting accounts to Qdrant");

        return syncAccountEmbeddingsUseCase.forceResyncAll()
                .map(syncResult -> Response.ok(syncResult).build())
                .onFailure().recoverWithItem(error -> {
                    log.error("Error resyncing all accounting accounts", error);
                    ErrorResponse errorResponse = new ErrorResponse(
                            500,
                            "Internal Server Error",
                            "Error al re-sincronizar todas las cuentas: " + error.getMessage()
                    );
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity(errorResponse)
                            .build();
                });
    }

    // ==================== Historical Journal Entries Sync ====================

    @POST
    @Path("/sync-historical-entries")
    @Operation(
            summary = "Sincronizar asientos históricos a Qdrant",
            description = """
                    Sincroniza TODOS los asientos contables existentes a Qdrant para que el RAG
                    pueda aprender de registros históricos y mejorar las sugerencias futuras.

                    Ejecuta esto una vez después de instalar el sistema o cuando quieras
                    asegurarte de que todos los asientos están sincronizados.
                    """
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "Asientos sincronizados exitosamente",
                    content = @Content(schema = @Schema(implementation = EmbeddingGenerationResponse.class))
            ),
            @APIResponse(
                    responseCode = "500",
                    description = "Error al sincronizar asientos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public Uni<Response> syncAllHistoricalEntries() {
        log.info("Admin request: Sync all historical journal entries to Qdrant");
        long startTime = System.currentTimeMillis();

        return historicalEntriesSyncUseCase.syncAllEntries()
                .map(count -> {
                    long duration = System.currentTimeMillis() - startTime;
                    EmbeddingGenerationResponse response = new EmbeddingGenerationResponse(
                            count,
                            String.format("Successfully synced %d historical journal entries to Qdrant", count),
                            duration
                    );
                    return Response.ok(response).build();
                })
                .onFailure().recoverWithItem(error -> {
                    log.error("Error syncing historical entries", error);
                    ErrorResponse errorResponse = new ErrorResponse(
                            500,
                            "Internal Server Error",
                            "Error al sincronizar asientos históricos: " + error.getMessage()
                    );
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity(errorResponse)
                            .build();
                });
    }

    @POST
    @Path("/sync-historical-entry/{entryId}")
    @Operation(
            summary = "Sincronizar un asiento específico a Qdrant",
            description = "Sincroniza un asiento contable específico a Qdrant por su ID"
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "Asiento sincronizado exitosamente"
            ),
            @APIResponse(
                    responseCode = "404",
                    description = "Asiento no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @APIResponse(
                    responseCode = "500",
                    description = "Error al sincronizar asiento",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public Uni<Response> syncHistoricalEntry(@PathParam("entryId") Integer entryId) {
        log.info("Admin request: Sync historical journal entry: {}", entryId);
        long startTime = System.currentTimeMillis();

        return historicalEntriesSyncUseCase.syncEntry(entryId)
                .map(success -> {
                    long duration = System.currentTimeMillis() - startTime;
                    EmbeddingGenerationResponse response = new EmbeddingGenerationResponse(
                            1,
                            String.format("Successfully synced journal entry %d to Qdrant", entryId),
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
                    log.error("Error syncing journal entry: {}", entryId, error);
                    ErrorResponse errorResponse = new ErrorResponse(
                            500,
                            "Internal Server Error",
                            "Error al sincronizar asiento: " + error.getMessage()
                    );
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity(errorResponse)
                            .build();
                });
    }

}
