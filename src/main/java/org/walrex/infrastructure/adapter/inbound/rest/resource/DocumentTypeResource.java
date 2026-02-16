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
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.walrex.application.dto.response.DocumentTypeResponse;
import org.walrex.application.port.input.GetDocumentTypesUseCase;

import java.util.List;

@Slf4j
@Path("/api/v1/document-types")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Tag(name = "Document Types", description = "API para obtener los tipos de documento por país")
public class DocumentTypeResource {

    @Inject
    GetDocumentTypesUseCase getDocumentTypesUseCase;

    @GET
    @WithSession
    @Operation(
            summary = "Obtener tipos de documento por país",
            description = "Retorna una lista de tipos de documento válidos para el país especificado"
    )
    @APIResponse(
            responseCode = "200",
            description = "Lista de tipos de documento obtenida exitosamente",
            content = @Content(schema = @Schema(implementation = DocumentTypeResponse.class))
    )
    public Uni<Response> getDocumentTypes(@QueryParam("countryIso2") String countryIso2) {
        if (countryIso2 == null || countryIso2.isBlank()) {
            return Uni.createFrom().item(Response.status(Response.Status.BAD_REQUEST)
                    .entity("countryIso2 query parameter is required")
                    .build());
        }

        log.debug("GET /api/v1/document-types?countryIso2={}", countryIso2);

        return getDocumentTypesUseCase.getDocumentTypesByCountry(countryIso2.toUpperCase())
                .map(response -> Response.ok(response).build())
                .onFailure().invoke(err -> log.error("Error fetching document types for country: {}", countryIso2, err))
                .onFailure().recoverWithItem(err -> Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity("Error retrieving document types: " + err.getMessage())
                        .build());
    }
}
