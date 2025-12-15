package org.walrex.infrastructure.adapter.inbound.rest.router;

import io.quarkus.vertx.web.Route;
import io.quarkus.vertx.web.RouteBase;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.walrex.application.dto.request.CreateSunatDocumentTypeRequest;
import org.walrex.application.dto.request.UpdateSunatDocumentTypeRequest;
import org.walrex.application.dto.response.AvailabilityResponse;
import org.walrex.application.dto.response.PagedResponse;
import org.walrex.application.dto.response.SunatDocumentTypeResponse;

/**
 * Router REST para tipos de documentos SUNAT.
 *
 * Define las rutas HTTP y la documentación OpenAPI para el recurso SunatDocumentType.
 * Todas las operaciones son reactivas (retornan Uni<Void>).
 *
 * Endpoints disponibles:
 * - POST   /api/v1/sunat-document-types           - Crear nuevo tipo de documento
 * - GET    /api/v1/sunat-document-types           - Listar con paginación y filtros
 * - GET    /api/v1/sunat-document-types/{id}      - Obtener por ID
 * - PUT    /api/v1/sunat-document-types/{id}      - Actualizar
 * - DELETE /api/v1/sunat-document-types/{id}      - Desactivar
 * - GET    /api/v1/sunat-document-types/check-availability - Verificar disponibilidad
 */
@ApplicationScoped
@RouteBase(path = "/api/v1/sunat-document-types", produces = "application/json")
@Tag(name = "SUNAT Document Types", description = "API para gestión de tipos de documentos SUNAT")
public class SunatDocumentTypeRouter {

    @Inject
    SunatDocumentTypeHandler handler;

    @Route(path = "", methods = Route.HttpMethod.POST)
    @Operation(
            summary = "Crear un nuevo tipo de documento SUNAT",
            description = "Crea un nuevo tipo de documento de identidad según la clasificación de SUNAT"
    )
    @RequestBody(
            description = "Datos del tipo de documento a crear",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = CreateSunatDocumentTypeRequest.class),
                    examples = @ExampleObject(
                            name = "DNI",
                            value = """
                                    {
                                        "id": "01",
                                        "code": "1",
                                        "name": "DNI",
                                        "description": "Documento Nacional de Identidad",
                                        "length": 8,
                                        "pattern": "^[0-9]{8}$"
                                    }
                                    """
                    )
            )
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "201",
                    description = "Tipo de documento creado exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SunatDocumentTypeResponse.class)
                    )
            ),
            @APIResponse(
                    responseCode = "400",
                    description = "Datos inválidos o tipo de documento duplicado",
                    content = @Content(mediaType = "application/json")
            ),
            @APIResponse(
                    responseCode = "409",
                    description = "Conflicto - Ya existe un tipo de documento con ese ID o código",
                    content = @Content(mediaType = "application/json")
            )
    })
    public Uni<Void> create(RoutingContext rc) {
        return handler.create(rc);
    }

    /**
     * GET /api/v1/sunat-document-types - List all document types with pagination and filtering
     */
    @Route(path = "", methods = Route.HttpMethod.GET)
    @Operation(
            summary = "Listar tipos de documentos SUNAT con paginación",
            description = "Obtiene un listado paginado de tipos de documentos con soporte para filtros y ordenamiento."
    )
    @Parameter(
            name = "page",
            description = "Número de página (base 1, primera página = 1)",
            in = ParameterIn.QUERY,
            schema = @Schema(type = SchemaType.INTEGER, defaultValue = "1"),
            example = "1"
    )
    @Parameter(
            name = "size",
            description = "Tamaño de página (elementos por página)",
            in = ParameterIn.QUERY,
            schema = @Schema(type = SchemaType.INTEGER, defaultValue = "10", maximum = "100"),
            example = "10"
    )
    @Parameter(
            name = "sortBy",
            description = "Campo por el cual ordenar",
            in = ParameterIn.QUERY,
            schema = @Schema(type = SchemaType.STRING, defaultValue = "id"),
            example = "name"
    )
    @Parameter(
            name = "sortDirection",
            description = "Dirección del ordenamiento",
            in = ParameterIn.QUERY,
            schema = @Schema(type = SchemaType.STRING, defaultValue = "asc", enumeration = {"asc", "desc"}),
            required = false,
            example = "asc"
    )
    @Parameter(
            name = "search",
            description = "Búsqueda general en nombre, código o ID",
            in = ParameterIn.QUERY,
            schema = @Schema(type = SchemaType.STRING),
            required = false,
            example = "DNI"
    )
    @Parameter(
            name = "code",
            description = "Filtro exacto por código SUNAT",
            in = ParameterIn.QUERY,
            schema = @Schema(type = SchemaType.STRING),
            required = false,
            example = "1"
    )
    @Parameter(
            name = "active",
            description = "Filtro por estado activo/inactivo",
            in = ParameterIn.QUERY,
            schema = @Schema(type = SchemaType.BOOLEAN),
            required = false,
            example = "true"
    )
    @Parameter(
            name = "length",
            description = "Filtro por longitud del documento",
            in = ParameterIn.QUERY,
            schema = @Schema(type = SchemaType.INTEGER),
            required = false,
            example = "8"
    )
    @Parameter(
            name = "includeInactive",
            description = "Incluir registros inactivos",
            in = ParameterIn.QUERY,
            schema = @Schema(type = SchemaType.STRING, defaultValue = "1", enumeration = {"1", "0"}),
            required = false,
            example = "0"
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "Lista de tipos de documentos obtenida exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PagedResponse.class),
                            examples = @ExampleObject(
                                    name = "Respuesta paginada",
                                    value = """
                        {
                            "content": [
                                {
                                    "id": "01",
                                    "code": "1",
                                    "name": "DNI",
                                    "description": "Documento Nacional de Identidad",
                                    "length": 8,
                                    "pattern": "^[0-9]{8}$",
                                    "sunatUpdatedAt": null,
                                    "active": true,
                                    "createdAt": "2024-01-01T00:00:00Z",
                                    "updatedAt": "2024-01-01T00:00:00Z"
                                }
                            ],
                            "page": 1,
                            "size": 10,
                            "totalElements": 15,
                            "totalPages": 2,
                            "first": true,
                            "last": false,
                            "empty": false
                        }
                        """
                            )
                    )
            ),
            @APIResponse(
                    responseCode = "400",
                    description = "Parámetros de consulta inválidos",
                    content = @Content(mediaType = "application/json")
            )
    })
    public Uni<Void> list(RoutingContext rc) {
        return handler.list(rc);
    }

    /**
     * GET /api/v1/sunat-document-types/{id} - Get a document type by ID
     */
    @Route(path = "/:id", methods = Route.HttpMethod.GET)
    @Operation(
            summary = "Obtener tipo de documento por ID",
            description = "Recupera un tipo de documento específico por su identificador único"
    )
    @Parameter(
            name = "id",
            description = "ID único del tipo de documento",
            required = true,
            in = ParameterIn.PATH,
            schema = @Schema(type = SchemaType.STRING),
            example = "01"
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "Tipo de documento encontrado",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SunatDocumentTypeResponse.class)
                    )
            ),
            @APIResponse(
                    responseCode = "404",
                    description = "Tipo de documento no encontrado",
                    content = @Content(mediaType = "application/json")
            )
    })
    public Uni<Void> getById(RoutingContext rc) {
        return handler.getById(rc);
    }

    /**
     * PUT /api/v1/sunat-document-types/{id} - Update a document type
     */
    @Route(path = "/:id", methods = Route.HttpMethod.PUT)
    @Operation(
            summary = "Actualizar un tipo de documento",
            description = "Actualiza los datos de un tipo de documento existente"
    )
    @Parameter(
            name = "id",
            description = "ID único del tipo de documento a actualizar",
            in = ParameterIn.PATH,
            required = true,
            schema = @Schema(type = SchemaType.STRING),
            example = "01"
    )
    @RequestBody(
            description = "Datos actualizados del tipo de documento",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = UpdateSunatDocumentTypeRequest.class),
                    examples = @ExampleObject(
                            name = "Actualizar DNI",
                            value = """
                    {
                        "code": "1",
                        "name": "DNI - Documento Nacional de Identidad",
                        "description": "Documento Nacional de Identidad emitido por RENIEC",
                        "length": 8,
                        "pattern": "^[0-9]{8}$",
                        "active": true
                    }
                    """
                    )
            )
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "Tipo de documento actualizado exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SunatDocumentTypeResponse.class)
                    )
            ),
            @APIResponse(
                    responseCode = "400",
                    description = "Datos inválidos",
                    content = @Content(mediaType = "application/json")
            ),
            @APIResponse(
                    responseCode = "404",
                    description = "Tipo de documento no encontrado",
                    content = @Content(mediaType = "application/json")
            ),
            @APIResponse(
                    responseCode = "409",
                    description = "Conflicto - Ya existe otro tipo de documento con ese código",
                    content = @Content(mediaType = "application/json")
            )
    })
    public Uni<Void> update(RoutingContext rc) {
        return handler.update(rc);
    }

    /**
     * DELETE /api/v1/sunat-document-types/{id} - Deactivate a document type
     */
    @Route(path = "/:id", methods = Route.HttpMethod.DELETE)
    @Operation(
            summary = "Desactivar un tipo de documento",
            description = "Marca un tipo de documento como inactivo (active=false). " +
                    "No se elimina físicamente de la base de datos."
    )
    @Parameter(
            name = "id",
            description = "ID único del tipo de documento a desactivar",
            in = ParameterIn.PATH,
            required = true,
            schema = @Schema(type = SchemaType.STRING),
            example = "01"
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "204",
                    description = "Tipo de documento desactivado exitosamente (No Content)"
            ),
            @APIResponse(
                    responseCode = "404",
                    description = "Tipo de documento no encontrado",
                    content = @Content(mediaType = "application/json")
            )
    })
    public Uni<Void> delete(RoutingContext rc) {
        return handler.delete(rc);
    }

    /**
     * GET /api/v1/sunat-document-types/check-availability - Check if a field value is available
     * Query params: id, code
     */
    @Route(path = "/check-availability", methods = Route.HttpMethod.GET)
    @Operation(
            summary = "Verificar disponibilidad de un campo",
            description = "Verifica si un valor específico (ID o código) está disponible o ya existe. " +
                    "Útil para validaciones en tiempo real."
    )
    @Parameter(
            name = "id",
            description = "ID a verificar",
            in = ParameterIn.QUERY,
            schema = @Schema(type = SchemaType.STRING),
            example = "01"
    )
    @Parameter(
            name = "code",
            description = "Código SUNAT a verificar",
            in = ParameterIn.QUERY,
            schema = @Schema(type = SchemaType.STRING),
            example = "1"
    )
    @Parameter(
            name = "excludeId",
            description = "ID a excluir de la verificación (útil para actualizaciones)",
            in = ParameterIn.QUERY,
            schema = @Schema(type = SchemaType.STRING),
            example = "01"
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "Verificación completada",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AvailabilityResponse.class),
                            examples = @ExampleObject(
                                    name = "Campo disponible",
                                    value = """
                        {
                            "field": "id",
                            "value": "01",
                            "available": true
                        }
                        """
                            )
                    )
            ),
            @APIResponse(
                    responseCode = "400",
                    description = "Parámetros inválidos - Se requiere al menos un campo a verificar",
                    content = @Content(mediaType = "application/json")
            )
    })
    public Uni<Void> checkAvailability(RoutingContext rc) {
        return handler.checkAvailability(rc);
    }
}
