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
import org.walrex.application.dto.request.CreateSystemDocumentTypeRequest;
import org.walrex.application.dto.request.UpdateSystemDocumentTypeRequest;
import org.walrex.application.dto.response.AvailabilityResponse;
import org.walrex.application.dto.response.PagedResponse;
import org.walrex.application.dto.response.SystemDocumentTypeResponse;

@ApplicationScoped
@RouteBase(path = "/api/v1/system-document-types", produces = "application/json")
@Tag(name = "System Document Types", description = "API para gestión de tipos de documento del sistema")
public class SystemDocumentTypeRouter {

    @Inject
    SystemDocumentTypeHandler systemDocumentTypeHandler;

    /**
     * POST /api/v1/system-document-types - Create new system document type
     */
    @Route(path = "", methods = Route.HttpMethod.POST)
    @Operation(summary = "Crear un nuevo tipo de documento", description = "Crea un nuevo tipo de documento del sistema")
    @RequestBody(description = "Datos del tipo de documento a crear", required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = CreateSystemDocumentTypeRequest.class), examples = @ExampleObject(name = "DPI Guatemala", value = """
            {
                "code": "DPI",
                "name": "Documento Personal de Identificación",
                "description": "Documento de identidad nacional de Guatemala",
                "isRequired": true,
                "forPerson": true,
                "forCompany": false,
                "priority": 1
            }
            """)))
    @APIResponses({
            @APIResponse(responseCode = "201", description = "Tipo de documento creado exitosamente", content = @Content(mediaType = "application/json", schema = @Schema(implementation = SystemDocumentTypeResponse.class))),
            @APIResponse(responseCode = "400", description = "Datos inválidos", content = @Content(mediaType = "application/json")),
            @APIResponse(responseCode = "409", description = "Conflicto - Ya existe un tipo de documento con ese código o nombre", content = @Content(mediaType = "application/json"))
    })
    public Uni<Void> create(RoutingContext rc) {
        return systemDocumentTypeHandler.create(rc);
    }

    /**
     * GET /api/v1/system-document-types - List all system document types with
     * pagination and filtering
     */
    @Route(path = "", methods = Route.HttpMethod.GET)
    @Operation(summary = "Listar tipos de documento con paginación", description = "Obtiene un listado paginado de tipos de documento con soporte para filtros y ordenamiento. "
            +
            "Los resultados se cachean en Redis por 5 minutos para mejorar el rendimiento.")
    @Parameter(name = "page", description = "Número de página (base 1, primera página = 1)", in = ParameterIn.QUERY, schema = @Schema(type = SchemaType.INTEGER, defaultValue = "1"), example = "1")
    @Parameter(name = "size", description = "Tamaño de página (elementos por página)", in = ParameterIn.QUERY, schema = @Schema(type = SchemaType.INTEGER, defaultValue = "10", maximum = "100"), example = "10")
    @Parameter(name = "sortBy", description = "Campo por el cual ordenar", in = ParameterIn.QUERY, schema = @Schema(type = SchemaType.STRING, defaultValue = "id"), example = "priority")
    @Parameter(name = "sortDirection", description = "Dirección del ordenamiento", in = ParameterIn.QUERY, schema = @Schema(type = SchemaType.STRING, defaultValue = "asc", enumeration = {
            "asc", "desc" }), required = false, example = "asc")
    @Parameter(name = "search", description = "Búsqueda general en código, nombre y descripción", in = ParameterIn.QUERY, schema = @Schema(type = SchemaType.STRING), required = false, example = "DPI")
    @Parameter(name = "code", description = "Filtro por código exacto", in = ParameterIn.QUERY, schema = @Schema(type = SchemaType.STRING), required = false, example = "DPI")
    @Parameter(name = "isRequired", description = "Filtro por si es requerido", in = ParameterIn.QUERY, schema = @Schema(type = SchemaType.BOOLEAN), required = false, example = "true")
    @Parameter(name = "forPerson", description = "Filtro por si aplica para personas", in = ParameterIn.QUERY, schema = @Schema(type = SchemaType.BOOLEAN), required = false, example = "true")
    @Parameter(name = "forCompany", description = "Filtro por si aplica para empresas", in = ParameterIn.QUERY, schema = @Schema(type = SchemaType.BOOLEAN), required = false, example = "false")
    @Parameter(name = "active", description = "Filtro por estado activo", in = ParameterIn.QUERY, schema = @Schema(type = SchemaType.BOOLEAN), required = false, example = "true")
    @Parameter(name = "includeDeleted", description = "Campo para agregar registros deshabilitados", in = ParameterIn.QUERY, schema = @Schema(type = SchemaType.STRING, defaultValue = "0", enumeration = {
            "1", "0" }), required = false, example = "0")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Lista de tipos de documento obtenida exitosamente", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PagedResponse.class), examples = @ExampleObject(name = "Respuesta paginada", value = """
                    {
                        "content": [
                            {
                                "id": 1,
                                "code": "DPI",
                                "name": "Documento Personal de Identificación",
                                "description": "Documento de identidad nacional de Guatemala",
                                "isRequired": true,
                                "forPerson": true,
                                "forCompany": false,
                                "priority": 1,
                                "active": true,
                                "createdAt": "2024-01-01T00:00:00Z",
                                "updatedAt": "2024-01-01T00:00:00Z"
                            }
                        ],
                        "page": 1,
                        "size": 10,
                        "totalElements": 5,
                        "totalPages": 1,
                        "first": true,
                        "last": true,
                        "empty": false
                    }
                    """))),
            @APIResponse(responseCode = "400", description = "Parámetros de consulta inválidos", content = @Content(mediaType = "application/json"))
    })
    public Uni<Void> list(RoutingContext rc) {
        return systemDocumentTypeHandler.list(rc);
    }

    /**
     * GET /api/v1/system-document-types/{id} - Get a system document type by ID
     */
    @Route(path = "/:id", methods = Route.HttpMethod.GET)
    @Operation(summary = "Obtener tipo de documento por ID", description = "Recupera un tipo de documento específico por su identificador único")
    @Parameter(name = "id", description = "ID único del tipo de documento", required = true, in = ParameterIn.PATH, schema = @Schema(type = SchemaType.INTEGER), example = "1")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Tipo de documento encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = SystemDocumentTypeResponse.class))),
            @APIResponse(responseCode = "404", description = "Tipo de documento no encontrado", content = @Content(mediaType = "application/json"))
    })
    public Uni<Void> getById(RoutingContext rc) {
        return systemDocumentTypeHandler.getById(rc);
    }

    /**
     * PUT /api/v1/system-document-types/{id} - Update a system document type
     */
    @Route(path = "/:id", methods = Route.HttpMethod.PUT)
    @Operation(summary = "Actualizar un tipo de documento", description = "Actualiza los datos de un tipo de documento existente. Invalida automáticamente el cache.")
    @Parameter(name = "id", description = "ID único del tipo de documento a actualizar", in = ParameterIn.PATH, required = true, schema = @Schema(type = SchemaType.INTEGER), example = "1")
    @RequestBody(description = "Datos actualizados del tipo de documento", required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = UpdateSystemDocumentTypeRequest.class), examples = @ExampleObject(name = "Actualizar tipo de documento", value = """
            {
                "code": "DPI",
                "name": "Documento Personal de Identificación",
                "description": "Documento de identidad nacional de Guatemala - Actualizado",
                "isRequired": true,
                "forPerson": true,
                "forCompany": false,
                "priority": 1,
                "active": true
            }
            """)))
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Tipo de documento actualizado exitosamente", content = @Content(mediaType = "application/json", schema = @Schema(implementation = SystemDocumentTypeResponse.class))),
            @APIResponse(responseCode = "400", description = "Datos inválidos", content = @Content(mediaType = "application/json")),
            @APIResponse(responseCode = "404", description = "Tipo de documento no encontrado", content = @Content(mediaType = "application/json")),
            @APIResponse(responseCode = "409", description = "Conflicto - Ya existe otro tipo de documento con ese código o nombre", content = @Content(mediaType = "application/json"))
    })
    public Uni<Void> update(RoutingContext rc) {
        return systemDocumentTypeHandler.update(rc);
    }

    /**
     * DELETE /api/v1/system-document-types/{id} - Delete a system document type
     * (soft delete)
     */
    @Route(path = "/:id", methods = Route.HttpMethod.DELETE)
    @Operation(summary = "Eliminar un tipo de documento (soft delete)", description = "Marca un tipo de documento como eliminado sin removerlo físicamente de la base de datos. "
            +
            "Invalida automáticamente el cache.")
    @Parameter(name = "id", description = "ID único del tipo de documento a eliminar", in = ParameterIn.PATH, required = true, schema = @Schema(type = SchemaType.INTEGER), example = "1")
    @APIResponses({
            @APIResponse(responseCode = "204", description = "Tipo de documento eliminado exitosamente (No Content)"),
            @APIResponse(responseCode = "404", description = "Tipo de documento no encontrado", content = @Content(mediaType = "application/json"))
    })
    public Uni<Void> delete(RoutingContext rc) {
        return systemDocumentTypeHandler.delete(rc);
    }

    /**
     * GET /api/v1/system-document-types/check-availability - Check if a field value
     * is available
     */
    @Route(path = "/check-availability", methods = Route.HttpMethod.GET)
    @Operation(summary = "Verificar disponibilidad de un campo", description = "Verifica si un valor específico (código o nombre) "
            +
            "está disponible o ya existe en otro tipo de documento. Útil para validaciones en tiempo real.")
    @Parameter(name = "code", description = "Código a verificar (ej: DPI)", in = ParameterIn.QUERY, schema = @Schema(type = SchemaType.STRING), example = "DPI")
    @Parameter(name = "name", description = "Nombre del tipo de documento a verificar", in = ParameterIn.QUERY, schema = @Schema(type = SchemaType.STRING), example = "Documento Personal de Identificación")
    @Parameter(name = "excludeId", description = "ID de tipo de documento a excluir de la verificación (útil para actualizaciones)", in = ParameterIn.QUERY, schema = @Schema(type = SchemaType.INTEGER), example = "1")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Verificación completada", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AvailabilityResponse.class), examples = @ExampleObject(name = "Campo disponible", value = """
                    {
                        "field": "code",
                        "value": "DPI",
                        "available": true
                    }
                    """))),
            @APIResponse(responseCode = "400", description = "Parámetros inválidos - Se requiere al menos un campo a verificar", content = @Content(mediaType = "application/json"))
    })
    public Uni<Void> checkAvailability(RoutingContext rc) {
        return systemDocumentTypeHandler.checkAvailability(rc);
    }
}
