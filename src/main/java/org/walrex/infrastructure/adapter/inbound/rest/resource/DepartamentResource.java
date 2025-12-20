package org.walrex.infrastructure.adapter.inbound.rest.resource;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.walrex.application.dto.query.DepartamentFilter;
import org.walrex.application.dto.query.PageRequest;
import org.walrex.application.dto.request.CreateDepartamentRequest;
import org.walrex.application.dto.request.UpdateDepartamentRequest;
import org.walrex.application.dto.response.DepartamentResponse;
import org.walrex.application.dto.response.ErrorResponse;
import org.walrex.application.dto.response.PagedResponse;
import org.walrex.application.port.input.*;
import org.walrex.domain.model.Departament;
import org.walrex.infrastructure.adapter.inbound.mapper.DepartamentDtoMapper;
import org.walrex.infrastructure.adapter.inbound.mapper.DepartamentRequestMapper;

import java.net.URI;

@Path("/api/v1/ubigeo/departments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Tag(name = "Departamentos", description = "Gestión de Departamentos Regionales")
public class DepartamentResource {

    @Inject
    CreateDepartamentUseCase createUC;

    @Inject
    UpdateDepartamentUseCase updateUC;

    @Inject
    DeleteDepartamentUseCase deleteUC;

    @Inject
    GetDepartmentRegionalUseCase getUC;

    @Inject
    ListDepartmentReginalUseCase listUC;

    @Inject
    DepartamentRequestMapper requestMapper;

    @Inject
    DepartamentDtoMapper dtoMapper;

    @GET
    @WithSession
    @Operation(summary = "Listar departamentos", description = "Obtiene una lista paginada de departamentos con filtros opcionales")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Lista de departamentos obtenida exitosamente", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PagedResponse.class))),
            @APIResponse(responseCode = "500", description = "Error interno del servidor", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Uni<Response> list(
            @Parameter(description = "Número de página (1-based)", example = "1") @QueryParam("page") @DefaultValue("1") int page,
            @Parameter(description = "Tamaño de página", example = "10") @QueryParam("size") @DefaultValue("10") int size,
            @Parameter(description = "Búsqueda por nombre") @QueryParam("search") String search,
            @Parameter(description = "Búsqueda por código exacto") @QueryParam("code") String code,
            @Parameter(description = "Campo de ordenamiento", example = "id") @QueryParam("sortBy") @DefaultValue("id") String sortBy,
            @Parameter(description = "Dirección de ordenamiento (ASC/DESC)", example = "ASC") @QueryParam("sortDirection") @DefaultValue("ASC") String sortDirection) {
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

        DepartamentFilter filter = DepartamentFilter.builder()
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
    @Operation(summary = "Obtener departamento por ID", description = "Retorna un único departamento basado en su ID")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Departamento encontrado", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = DepartamentResponse.class))),
            @APIResponse(responseCode = "404", description = "Departamento no encontrado", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = "500", description = "Error interno del servidor", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Uni<Response> getById(
            @Parameter(description = "ID del departamento", required = true) @PathParam("id") Integer id) {
        return getUC.findById(id)
                .map(departament -> dtoMapper.toResponse(departament))
                .map(response -> Response.ok(response).build())
                .onFailure().recoverWithItem(this::mapExceptionToResponse);
    }

    @POST
    @WithTransaction
    @Operation(summary = "Crear departamento", description = "Crea un nuevo departamento")
    @APIResponses({
            @APIResponse(responseCode = "201", description = "Departamento creado exitosamente", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = DepartamentResponse.class))),
            @APIResponse(responseCode = "400", description = "Datos de entrada inválidos", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = "409", description = "Conflicto: El código o nombre ya existen", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = "500", description = "Error interno del servidor", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Uni<Response> create(
            @RequestBody(description = "Datos del nuevo departamento", required = true) @Valid CreateDepartamentRequest request) {
        Departament departament = requestMapper.toModel(request);
        return createUC.agregar(departament)
                .map(created -> dtoMapper.toResponse(created))
                .map(response -> Response.created(URI.create("/api/v1/departamentos/" + response.id()))
                        .entity(response)
                        .build())
                .onFailure().recoverWithItem(this::mapExceptionToResponse);
    }

    @PUT
    @Path("/{id}")
    @WithTransaction
    @Operation(summary = "Actualizar departamento", description = "Actualiza los datos de un departamento existente")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Departamento actualizado exitosamente", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = DepartamentResponse.class))),
            @APIResponse(responseCode = "400", description = "Datos de entrada inválidos", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = "404", description = "Departamento no encontrado", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = "409", description = "Conflicto: El código o nombre ya existen en otro registro", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = "500", description = "Error interno del servidor", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Uni<Response> update(
            @Parameter(description = "ID del departamento a actualizar", required = true) @PathParam("id") Integer id,
            @RequestBody(description = "Datos actualizados del departamento", required = true) @Valid UpdateDepartamentRequest request) {
        Departament departament = requestMapper.toModel(request);
        return updateUC.execute(id, departament)
                .map(updated -> dtoMapper.toResponse(updated))
                .map(response -> Response.ok(response).build())
                .onFailure().recoverWithItem(this::mapExceptionToResponse);
    }

    @DELETE
    @Path("/{id}")
    @WithTransaction
    @Operation(summary = "Eliminar departamento", description = "Realiza un borrado lógico del departamento")
    @APIResponses({
            @APIResponse(responseCode = "204", description = "Departamento eliminado exitosamente"),
            @APIResponse(responseCode = "404", description = "Departamento no encontrado", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = "500", description = "Error interno del servidor", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Uni<Response> delete(
            @Parameter(description = "ID del departamento a eliminar", required = true) @PathParam("id") Integer id) {
        return deleteUC.deshabilitar(id)
                .map(unused -> Response.noContent().build())
                .onFailure().recoverWithItem(this::mapExceptionToResponse);
    }

    @GET
    @Path("/stream")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @Operation(summary = "Stream de departamentos", description = "Obtiene un stream de todos los departamentos activos")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Stream iniciado", content = @Content(mediaType = MediaType.SERVER_SENT_EVENTS, schema = @Schema(implementation = DepartamentResponse.class)))
    })
    public Multi<DepartamentResponse> stream() {
        return listUC.streamAll();
    }

    private Response mapExceptionToResponse(Throwable t) {
        Response.Status status;

        if (t instanceof org.walrex.domain.exception.DepartamentNotFoundException) {
            status = Response.Status.NOT_FOUND;
        } else if (t instanceof org.walrex.domain.exception.DuplicateDepartamentException) {
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
