package org.walrex.infrastructure.adapter.outbound.azure;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.walrex.infrastructure.adapter.outbound.azure.dto.AzureAnalyzeResponse;

/**
 * REST Client para Azure Document Intelligence API.
 * Usa Quarkus REST Client reactivo con Mutiny.
 */
@Path("/formrecognizer/documentModels")
@RegisterRestClient(configKey = "azure-doc-intel")
@Produces(MediaType.APPLICATION_JSON)
public interface AzureDocIntelRestClient {

    /**
     * Inicia el análisis de un documento usando el modelo prebuilt-invoice.
     *
     * Azure Document Intelligence API usa un patrón asíncrono:
     * 1. POST inicia el análisis y retorna 202 Accepted con Operation-Location header
     * 2. GET al Operation-Location retorna el resultado cuando está listo
     *
     * @param modelId Modelo a utilizar (ej: "prebuilt-invoice")
     * @param apiKey  Clave de API de Azure
     * @param contentType Tipo de contenido del documento
     * @param apiVersion Versión de la API
     * @param documentBytes Contenido del documento
     * @return Uni con la respuesta completa (status 202 + headers)
     */
    @POST
    @Path("/{modelId}:analyze")
    Uni<Response> analyzeDocument(
            @PathParam("modelId") String modelId,
            @HeaderParam("Ocp-Apim-Subscription-Key") String apiKey,
            @HeaderParam("Content-Type") String contentType,
            @QueryParam("api-version") String apiVersion,
            byte[] documentBytes
    );

    /**
     * Obtiene el resultado del análisis usando el resultId.
     *
     * @param modelId    Modelo utilizado (ej: "prebuilt-invoice")
     * @param resultId   ID del resultado (extraído del Operation-Location)
     * @param apiKey     Clave de API de Azure
     * @param apiVersion Versión de la API
     * @return Uni con el resultado del análisis
     */
    @GET
    @Path("/{modelId}/analyzeResults/{resultId}")
    Uni<AzureAnalyzeResponse> getAnalysisResult(
            @PathParam("modelId") String modelId,
            @PathParam("resultId") String resultId,
            @HeaderParam("Ocp-Apim-Subscription-Key") String apiKey,
            @QueryParam("api-version") String apiVersion
    );
}
