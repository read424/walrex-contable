package org.walrex.infrastructure.adapter.inbound.rest.resource;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.walrex.application.dto.query.PageRequest;
import org.walrex.application.dto.query.ProvinceFilter;
import org.walrex.application.dto.request.CreateProvinceRequest;
import org.walrex.application.dto.request.UpdateProvinceRequest;
import org.walrex.application.dto.response.ErrorResponse;
import org.walrex.application.dto.response.PagedResponse;
import org.walrex.application.dto.response.ProvinceResponse;
import org.walrex.application.port.input.*;
import org.walrex.domain.model.Province;
import org.walrex.infrastructure.adapter.inbound.mapper.ProvinceDtoMapper;
import org.walrex.infrastructure.adapter.inbound.mapper.ProvinceRequestMapper;

import java.net.URI;

@Path("/api/v1/ubigeo/province")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Provincias", description = "Gestión de Provincias Regionales")
public class ProvinceResource {

    @Inject
    CreateProvinceUseCase createUC;

    @Inject
    UpdateProvinceUseCase updateUC;

    @Inject
    DeleteProvinceUseCase deleteUC;

    @Inject
    GetProvinceUseCase getUC;

    @Inject
    ListProvinceRegionalUseCase listUC;

    @Inject
    ProvinceRequestMapper requestMapper;

    @Inject
    ProvinceDtoMapper dtoMapper;

    @GET
    @WithSession
    @Operation(summary = "Listar provincias", description = "Obtiene una lista paginada de provincias con filtros opcionales")
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "Lista de provincias obtenida exitosamente",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = PagedResponse.class)
                    )
            ),
            @APIResponse(
                    responseCode = "500",
                    description = "Error interno del servidor",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    public Uni<Response> list(
        @Parameter(description = "Número de página (1-based", example = "1")
        @QueryParam("page")
        @DefaultValue("1")
        Integer page,
        @Parameter(description = "Tamaño de página", example = "10")
        @QueryParam("size") @DefaultValue("10") Integer size,
        @Parameter(description = "Búsqueda por nombre")
        @QueryParam("search") String search,
        @Parameter(description = "Búsqueda por Departamento")
        @QueryParam("idDepartment") Integer idDepartment,
        @Parameter(description = "Búsqueda por código exacto")
        @QueryParam("code") String code,
        @Parameter(description = "Campo de ordenamiento", example = "id")
        @QueryParam("sortBy") @DefaultValue("id") String sortBy,
        @Parameter(description = "Dirección de ordenamiento (ASC/DESC)", example = "ASC", schema = @Schema(
                type = SchemaType.STRING, defaultValue = "asc", enumeration = {"asc", "desc"}
        ))
        @QueryParam("sortDirection") @DefaultValue("ASC") String sortDirection
    ){
        // Convert 1-based page to 0-based
        int pageIndex = Math.max(0, page - 1);

        PageRequest.SortDirection dir = "DESC".equalsIgnoreCase(sortDirection)
                ? PageRequest.SortDirection.DESCENDING
                : PageRequest.SortDirection.ASCENDING;

        PageRequest pageRequest = PageRequest.builder()
                .page(pageIndex)
                .size(size)
                .sortBy(sortBy)
                .sortDirection(dir)
                .build();

        ProvinceFilter filter = ProvinceFilter.builder()
                .idDepartament(idDepartment)
                .name(search)
                .codigo(code)
                .build();

        return listUC.listar(pageRequest, filter)
                .map(pagedResponse -> Response.ok(pagedResponse).build())
                .onFailure().recoverWithItem(this::mapExceptionToResponse);
    }

    @GET
    @Path("/{id}")
    @WithSession
    @Operation(summary = "Obtener provincia por ID", description = "Retorna una única provincia basada en su ID")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Provincia encontrada", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProvinceResponse.class))),
            @APIResponse(responseCode = "404", description = "Provincia no encontrada", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = "500", description = "Error interno del servidor", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Uni<Response> getById(
            @Parameter(description = "ID de la provincia", required = true) @PathParam("id") Integer id) {
        return getUC.findById(id)
                .map(province -> dtoMapper.toResponse(province))
                .map(response -> Response.ok(response).build())
                .onFailure().recoverWithItem(this::mapExceptionToResponse);
    }

    @POST
    @WithTransaction
    @Operation(summary = "Crear provincia", description = "Crea una nueva provincia")
    @APIResponses({
            @APIResponse(responseCode = "201", description = "Provincia creada exitosamente", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProvinceResponse.class))),
            @APIResponse(responseCode = "400", description = "Datos de entrada inválidos", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = "409", description = "Conflicto: El código o nombre ya existen", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = "500", description = "Error interno del servidor", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Uni<Response> create(
            @RequestBody(description = "Datos de la nueva provincia", required = true) @Valid CreateProvinceRequest request) {
        Province province = requestMapper.toModel(request);
        return createUC.agregar(province)
                .map(created -> dtoMapper.toResponse(created))
                .map(response -> Response.created(URI.create("/api/v1/ubigeo/province/" + response.id()))
                        .entity(response)
                        .build())
                .onFailure().recoverWithItem(this::mapExceptionToResponse);
    }

    @PUT
    @Path("/{id}")
    @WithTransaction
    @Operation(summary = "Actualizar provincia", description = "Actualiza los datos de una provincia existente")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Provincia actualizada exitosamente", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProvinceResponse.class))),
            @APIResponse(responseCode = "400", description = "Datos de entrada inválidos", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = "404", description = "Provincia no encontrada", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = "409", description = "Conflicto: El código o nombre ya existen en otro registro", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = "500", description = "Error interno del servidor", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Uni<Response> update(
            @Parameter(description = "ID de la provincia a actualizar", required = true) @PathParam("id") Integer id,
            @RequestBody(description = "Datos actualizados de la provincia", required = true) @Valid UpdateProvinceRequest request) {
        Province province = requestMapper.toModel(request);
        return updateUC.execute(id, province)
                .map(updated -> dtoMapper.toResponse(updated))
                .map(response -> Response.ok(response).build())
                .onFailure().recoverWithItem(this::mapExceptionToResponse);
    }

    @DELETE
    @Path("/{id}")
    @WithTransaction
    @Operation(summary = "Eliminar provincia", description = "Realiza un borrado lógico de la provincia")
    @APIResponses({
            @APIResponse(responseCode = "204", description = "Provincia eliminada exitosamente"),
            @APIResponse(responseCode = "404", description = "Provincia no encontrada", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = "500", description = "Error interno del servidor", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Uni<Response> delete(
            @Parameter(description = "ID de la provincia a eliminar", required = true) @PathParam("id") Integer id) {
        return deleteUC.deshabilitar(id)
                .map(unused -> Response.noContent().build())
                .onFailure().recoverWithItem(this::mapExceptionToResponse);
    }

    @GET
    @Path("/stream")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @Operation(summary = "Stream de provincias", description = "Obtiene un stream de todas las provincias activas")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Stream iniciado", content = @Content(mediaType = MediaType.SERVER_SENT_EVENTS, schema = @Schema(implementation = ProvinceResponse.class)))
    })
    public Multi<ProvinceResponse> stream() {
        return listUC.streamAll();
    }

    private Response mapExceptionToResponse(Throwable t) {
        Response.Status status;

        if (t instanceof org.walrex.domain.exception.ProvinceNotFoundException) {
            status = Response.Status.NOT_FOUND;
        } else if (t instanceof org.walrex.domain.exception.DuplicateProvinceException) {
            status = Response.Status.CONFLICT;
        } else if (t instanceof IllegalArgumentException) {
            status = Response.Status.BAD_REQUEST;
        } else {
            status = Response.Status.INTERNAL_SERVER_ERROR;
        }

        return Response.status(status)
                .entity(new ErrorResponse(status.getStatusCode(), status.getReasonPhrase(), t.getMessage()))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
