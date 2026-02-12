package org.walrex.infrastructure.adapter.inbound.rest;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.walrex.application.dto.request.DecodeQrRequest;
import org.walrex.application.dto.request.GenerateQrRequest;
import org.walrex.application.dto.response.DecodeQrResponse;
import org.walrex.application.port.input.EmvQrUseCase;

@Path("/v1/qr-emv")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Tag(name = "EMV QR", description = "Operaciones para codificación y decodificación de códigos QR EMV")
public class EmvQrResource {

    @Inject
    EmvQrUseCase emvQrUseCase;

    @Inject
    org.walrex.application.port.input.SaveMerchantQrUseCase saveMerchantQrUseCase;

    @POST
    @Path("/generate")
    @Operation(summary = "Generar texto de QR EMV", description = "Genera un string de código QR EMV basado en los datos del negocio y monto")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Generación exitosa", 
                    content = @Content(mediaType = MediaType.TEXT_PLAIN, schema = @Schema(implementation = String.class))),
            @APIResponse(responseCode = "500", description = "Error interno")
    })
    public Uni<Response> generate(
            @RequestBody(description = "Datos para generar el QR", required = true,
                         content = @Content(schema = @Schema(implementation = GenerateQrRequest.class)))
            GenerateQrRequest request) {
        return emvQrUseCase.generateQr(request)
                .map(qr -> Response.ok(qr).build());
    }

    @POST
    @Path("/decode")
    @Operation(summary = "Decodificar texto de QR EMV", description = "Decodifica un string de QR EMV y valida su integridad mediante CRC16")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Datos decodificados", 
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = DecodeQrResponse.class))),
            @APIResponse(responseCode = "500", description = "Error interno")
    })
    public Uni<Response> decode(
            @RequestBody(description = "Objeto JSON con el texto del QR a decodificar", required = true, 
                         content = @Content(schema = @Schema(implementation = DecodeQrRequest.class)))
            DecodeQrRequest request) {
        return emvQrUseCase.decodeQr(request.getQrCodeText())
                .map(response -> Response.ok(response).build());
    }

    @POST
    @Path("/merchant")
    @Operation(summary = "Guardar perfil de comercio (Yape/Plin)", description = "Guarda los datos extraídos de un QR decodificado para su uso posterior")
    public Uni<Response> saveMerchant(@RequestBody org.walrex.application.dto.request.SaveMerchantQrRequest request) {
        return saveMerchantQrUseCase.saveMerchantQr(request)
                .map(merchant -> Response.ok(merchant).build());
    }

    @GET
    @Path("/merchant")
    @Operation(summary = "Listar perfiles de comercio guardados")
    public Uni<Response> listMerchants() {
        return saveMerchantQrUseCase.getAllMerchantQrs()
                .map(list -> Response.ok(list).build());
    }

    @GET
    @Path("/merchant/{id}/generate")
    @Operation(summary = "Generar QR desde un perfil guardado", description = "Genera el texto del QR EMV usando los datos del perfil y un monto opcional")
    public Uni<Response> generateFromProfile(@PathParam("id") Long id, @QueryParam("amount") java.math.BigDecimal amount) {
        return saveMerchantQrUseCase.generateFromProfile(id, amount)
                .map(qr -> Response.ok(qr).build());
    }

    @DELETE
    @Path("/merchant/{id}")
    @Operation(summary = "Eliminar perfil de comercio")
    public Uni<Response> deleteMerchant(@PathParam("id") Long id) {
        return saveMerchantQrUseCase.deleteMerchantQr(id)
                .map(deleted -> Response.ok(deleted).build());
    }
}
