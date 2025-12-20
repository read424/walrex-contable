package org.walrex.infrastructure.adapter.inbound.rest.router;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.Json;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;
import org.walrex.application.dto.request.LoadDataINEIRequest;
import org.walrex.application.dto.response.UbigeoErrorResponse;
import org.walrex.application.port.input.LoadUbigeoDataUseCase;
import org.walrex.application.port.input.PreviewUbigeoImportUseCase;

import java.nio.file.Path;

@Slf4j
@ApplicationScoped
public class UbigeoINEIHandler {

    @Inject
    PreviewUbigeoImportUseCase previewUbigeoImportUseCase;

    @Inject
    LoadUbigeoDataUseCase loadUbigeoDataUseCase;

    public Uni<Void> previewFlattened(RoutingContext rc) {
        log.info("Handler: previewFlattened endpoint called");

        // Log para ver cuántos archivos llegaron en total
        log.info("Total de archivos recibidos: {}", rc.fileUploads().size());

        // Iterar para ver qué nombres de variables (keys) están llegando
        rc.fileUploads().forEach(fu -> {
            log.info("Campo detectado: '{}' | Archivo: '{}' | Tamaño: {} bytes | Ubicación: {}",
                    fu.name(), fu.fileName(), fu.size(), fu.uploadedFileName());
        });

        if(rc.fileUploads().isEmpty()){
            rc.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code())
                    .end("No se encontró ningún archivo en la petición");
            return Uni.createFrom().voidItem();
        }

        FileUpload upload = rc.fileUploads().stream()
                .filter(f-> f.name().equals("file"))
                .findFirst()
                .orElse(null);
        if(upload==null){
            log.warn("⚠️ No se encontró el campo 'file' en la petición");
            rc.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code()).end("No se encontró el campo 'file'");
            return Uni.createFrom().voidItem();
        }
        Path filePath = Path.of(upload.uploadedFileName());
        String originalFileName = upload.fileName();
        log.info("Archivo temporal listo para procesar en: {} | Nombre original: {}", filePath, originalFileName);

        return previewUbigeoImportUseCase.previewFlattened(filePath, originalFileName)
                .onItem().invoke(response -> {
                    rc.response()
                            .putHeader("Content-Type", MediaType.APPLICATION_JSON)
                            .end(Json.encode(response));
                })
                .onFailure().invoke(err -> {
                    log.error("Error en el proceso: ", err);
                    rc.response()
                            .setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                            .end("Error: " + err.getMessage());
                })
                .replaceWithVoid();
    }

    @WithTransaction
    public Uni<Void> registrar(RoutingContext rc) {
        log.info("Handler: registrar endpoint called");

        try {
            // Obtener el body como JSON y convertir a LoadDataINEIRequest
            LoadDataINEIRequest request = rc.body().asPojo(LoadDataINEIRequest.class);

            if (request == null || request.records() == null) {
                log.warn("Invalid request body");
                rc.response()
                        .setStatusCode(HttpResponseStatus.BAD_REQUEST.code())
                        .putHeader("Content-Type", MediaType.APPLICATION_JSON)
                        .end(Json.encode(UbigeoErrorResponse.of("No se enviaron registros")));
                return Uni.createFrom().voidItem();
            }

            log.info("Processing {} records", request.records().size());

            // Llamar al use case
            return loadUbigeoDataUseCase.loadData(request)
                    .onItem().invoke(response -> {
                        int statusCode;
                        if (response.getError() != null) {
                            // Determinar el código de estado según el error
                            if (response.getError().contains("ya existen")) {
                                statusCode = HttpResponseStatus.CONFLICT.code();
                            } else if (response.getError().contains("No se enviaron") ||
                                       response.getError().contains("inválido")) {
                                statusCode = HttpResponseStatus.BAD_REQUEST.code();
                            } else {
                                statusCode = HttpResponseStatus.INTERNAL_SERVER_ERROR.code();
                            }
                        } else {
                            statusCode = HttpResponseStatus.OK.code();
                        }

                        log.info("Responding with status {} and response: {}", statusCode, response);
                        rc.response()
                                .setStatusCode(statusCode)
                                .putHeader("Content-Type", MediaType.APPLICATION_JSON)
                                .end(Json.encode(response));
                    })
                    .onFailure().invoke(err -> {
                        log.error("Error processing load data request", err);
                        rc.response()
                                .setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                                .putHeader("Content-Type", MediaType.APPLICATION_JSON)
                                .end(Json.encode(UbigeoErrorResponse.of("Error al insertar registros")));
                    })
                    .replaceWithVoid();

        } catch (Exception e) {
            log.error("Error parsing request body", e);
            rc.response()
                    .setStatusCode(HttpResponseStatus.BAD_REQUEST.code())
                    .putHeader("Content-Type", MediaType.APPLICATION_JSON)
                    .end(Json.encode(UbigeoErrorResponse.of("Formato de solicitud inválido")));
            return Uni.createFrom().voidItem();
        }
    }
}
