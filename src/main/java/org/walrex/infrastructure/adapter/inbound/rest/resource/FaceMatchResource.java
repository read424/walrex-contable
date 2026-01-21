package org.walrex.infrastructure.adapter.inbound.rest.resource;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.walrex.application.dto.response.ErrorResponse;
import org.walrex.application.dto.response.MatchVerifyFaceResponse;
import org.walrex.application.port.input.VerifyFaceMatchUseCase;

@Slf4j
@Path("/api/v1/kyc")
@ApplicationScoped
@Tag(name = "KYC", description = "Endpoints para verificación de identidad (Know Your Customer)")
public class FaceMatchResource {

    @Inject
    VerifyFaceMatchUseCase verifyFaceMatchUseCase;

    @POST
    @Path("/verify-face")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Verificar coincidencia facial",
            description = """
                    Compara la imagen del documento de identidad con una selfie del usuario
                    para verificar que corresponden a la misma persona.
                    """
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "Comparación facial realizada exitosamente",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = MatchVerifyFaceResponse.class)
                    )
            ),
            @APIResponse(
                    responseCode = "400",
                    description = "Error en los datos de entrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @APIResponse(
                    responseCode = "500",
                    description = "Error interno del servidor",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public Uni<Response> verifyFace(
            @RestForm("document_image")
            @RequestBody(
                    description = "Imagen del documento de identidad (frente)",
                    required = true,
                    content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM)
            )
            FileUpload documentImage,
            @RestForm("selfie_image")
            @RequestBody(
                    description = "Imagen selfie del usuario",
                    required = true,
                    content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM)
            )
            FileUpload selfieImage,
            @RestForm("session_id")
            @RequestBody(
                    description = "Session Id user mobile",
                    required = false
            )
            String sessionId
    ) {
        log.info("Received face verification request - sessionId: {}, documentImage: {}, selfieImage: {}",
                sessionId,
                documentImage != null ? documentImage.fileName() : "NULL",
                selfieImage != null ? selfieImage.fileName() : "NULL");

        if (documentImage == null || selfieImage == null) {
            log.warn("Missing files - documentImage is null: {}, selfieImage is null: {}",
                    documentImage == null, selfieImage == null);
            return Uni.createFrom().item(
                    Response.status(Response.Status.BAD_REQUEST)
                            .entity(new ErrorResponse(
                                    400,
                                    "Bad Request",
                                    "Se requieren ambas imágenes: documentImage y selfieImage"))
                            .build()
            );
        }

        return verifyFaceMatchUseCase.compareFaces(
                        documentImage.uploadedFile().toFile(),
                        selfieImage.uploadedFile().toFile()
                )
                .map(response -> Response.ok(response).build())
                .onFailure().recoverWithItem(error -> {
                    log.error("Error during face verification: {}", error.getMessage(), error);
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity(new ErrorResponse(
                                    500,
                                    "Internal Server Error",
                                    "Error al verificar rostro: " + error.getMessage()))
                            .build();
                });
    }
}
