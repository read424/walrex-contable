package org.walrex.infrastructure.adapter.inbound.rest.router;

import io.quarkus.vertx.web.Route;
import io.quarkus.vertx.web.RouteBase;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.media.SchemaProperty;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.walrex.application.dto.request.LoadDataINEIRequest;
import org.walrex.application.dto.response.UbigeoFlattenedPreviewResponse;

@Slf4j
@ApplicationScoped
@RouteBase(path = "/api/v1/ubigeo/inei", produces = MediaType.APPLICATION_JSON)
@Tag(name = "UBIGEO INEI", description = "Carga/Actualización Másiva UBIGEO según INEI")
public class UbigeoINEIRouter {

    @Inject
    UbigeoINEIHandler ubigeoINEIHandler;

    @Route(path = "/load-save", methods = Route.HttpMethod.POST, produces = MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Registrar departamentos, provincias, distritos y UBIGEO",
            description = "Registrar Departamentos, Provincias y Distritos con UBIGEO"
    )
    @RequestBody(
            description = "Listado de registros a guardar",
            required = true,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = LoadDataINEIRequest.class),
                    examples = @ExampleObject(
                            name = "Ejemplo de importacion",
                            summary = "Ejemplo con un registro valido",
                            value = """
                                    {
                                        "records": [
                                            {
                                              "id": "150122",
                                              "departamento": "LIMA",
                                              "provincia": "LIMA",
                                              "distrito": "MIRAFLORES",
                                              "status": "SUCCESS"
                                            }
                                        ]
                                    }
                                    """
                    )
            )
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "Registros insertados correctamente",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            examples = @ExampleObject(
                                    value = """
                                            {
                                                "insertedCount": 1850,
                                                "message": "Registros insertados correctamente"
                                            }
                                            """
                            )
                    )
            ),
            @APIResponse(
                    responseCode = "400",
                    description = "No se enviaron registros",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            examples = @ExampleObject(
                                    value = """
                                            {
                                                "error": "No se enviaron registros"
                                            }
                                            """
                            )
                    )
            ),
            @APIResponse(
                    responseCode = "409",
                    description = "Algunos códigos UBIGEO ya existen",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            examples = @ExampleObject(
                                    value = """
                                            {
                                                "error": "Algunos códigos UBIGEO ya existen"
                                            }
                                            """
                            )
                    )
            ),
            @APIResponse(
                    responseCode = "500",
                    description = "Error al insertar registros",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            examples = @ExampleObject(
                                    value = """
                                            {
                                                "error": "Error al insertar registros"
                                            }
                                            """
                            )
                    )
            )
    })
    public Uni<Void> registrar(RoutingContext rc){
        log.info("🎯 Router: /load-save endpoint reached");
        return ubigeoINEIHandler.registrar(rc);
    }

    @Route(path = "/preview-flattened", methods = Route.HttpMethod.POST)
    @Operation(
            summary = "Mostrar registros de UBIGEO segun archivo INEI",
            description = "Muestra la informacion de Departamento, Provincia y/o Distrito mediante archivo INEI"
    )
    @RequestBody(
            description = "Archivo para cargar y actualizar UBIGEO",
            required = true,
            content = @Content(
                    mediaType = MediaType.MULTIPART_FORM_DATA,
                    schema = @Schema(
                            type = SchemaType.OBJECT,
                            properties = {
                                    @SchemaProperty(
                                            name = "file",
                                            format = "binary"
                                    )
                            }
                    )
            )
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "Archivo leído con éxito",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = UbigeoFlattenedPreviewResponse.class)
                    )
            ),
            @APIResponse(
                    responseCode = "400",
                    description = "Datos inválidos",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)
            )
    })
    public Uni<Void> previewFlattened(RoutingContext rc) {
        log.info("🎯 Router: /preview-flattened endpoint reached");
        return ubigeoINEIHandler.previewFlattened(rc);
    }
}
