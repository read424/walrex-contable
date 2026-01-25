package org.walrex.infrastructure.adapter.inbound.rest;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.inject.Inject;
import org.walrex.application.dto.request.OcupacionCreateRequest;
import org.walrex.application.dto.request.OcupacionUpdateRequest;
import org.walrex.application.dto.response.OcupacionResponse;
import org.walrex.application.port.input.OcupacionUseCase;
import org.walrex.infrastructure.adapter.inbound.mapper.OcupacionRestMapper;
import org.walrex.domain.model.Ocupacion;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.net.URI;
import java.util.stream.Collectors;

@Path("/api/v1/ocupaciones")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Ocupaciones", description = "Operaciones relacionadas con las ocupaciones")
public class OcupacionResource {

    @Inject
    OcupacionUseCase ocupacionUseCase;

    @Inject
    OcupacionRestMapper ocupacionRestMapper;

    @POST
    @Operation(summary = "Crear una nueva ocupación",
            description = "Registra una nueva ocupación en el sistema con un código y nombre únicos.")
    @APIResponse(responseCode = "201", description = "Ocupación creada exitosamente",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = OcupacionResponse.class)))
    @APIResponse(responseCode = "400", description = "Datos de entrada inválidos",
            content = @Content(mediaType = MediaType.APPLICATION_JSON))
    @APIResponse(responseCode = "409", description = "Ocupación con el mismo código o nombre ya existe",
            content = @Content(mediaType = MediaType.APPLICATION_JSON))
    public Uni<Response> createOcupacion(
            @RequestBody(description = "Datos para crear una nueva ocupación",
                    required = true, content = @Content(schema = @Schema(implementation = OcupacionCreateRequest.class)))
            OcupacionCreateRequest request) {
        return ocupacionUseCase.createOcupacion(request)
                .map(ocupacion -> Response.created(URI.create("/api/v1/ocupaciones/" + ocupacion.getId()))
                        .entity(ocupacionRestMapper.toResponse(ocupacion))
                        .build());
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Actualizar una ocupación existente",
            description = "Actualiza la información de una ocupación específica por su ID.")
    @APIResponse(responseCode = "200", description = "Ocupación actualizada exitosamente",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = OcupacionResponse.class)))
    @APIResponse(responseCode = "400", description = "Datos de entrada inválidos",
            content = @Content(mediaType = MediaType.APPLICATION_JSON))
    @APIResponse(responseCode = "404", description = "Ocupación no encontrada",
            content = @Content(mediaType = MediaType.APPLICATION_JSON))
    @APIResponse(responseCode = "409", description = "Ocupación con el mismo código o nombre ya existe",
            content = @Content(mediaType = MediaType.APPLICATION_JSON))
    public Uni<Response> updateOcupacion(
            @PathParam("id")
            @Parameter(description = "ID de la ocupación a actualizar", required = true)
            Long id,
            @RequestBody(description = "Datos para actualizar la ocupación",
                    required = true, content = @Content(schema = @Schema(implementation = OcupacionUpdateRequest.class)))
            OcupacionUpdateRequest request) {
        if (!id.equals(request.getId())) {
            return Uni.createFrom().item(Response.status(Response.Status.BAD_REQUEST)
                    .entity("El ID en la URL no coincide con el ID en el cuerpo de la solicitud.")
                    .build());
        }
        Ocupacion ocupacion = ocupacionRestMapper.toDomain(request);
        return ocupacionUseCase.updateOcupacion(id, ocupacion)
                .map(updatedOcupacion -> Response.ok(ocupacionRestMapper.toResponse(updatedOcupacion)).build());
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Eliminar una ocupación",
            description = "Realiza una eliminación lógica (desactivación) de una ocupación por su ID.")
    @APIResponse(responseCode = "204", description = "Ocupación eliminada lógicamente exitosamente")
    @APIResponse(responseCode = "404", description = "Ocupación no encontrada",
            content = @Content(mediaType = MediaType.APPLICATION_JSON))
    public Uni<Response> deleteOcupacion(
            @PathParam("id")
            @Parameter(description = "ID de la ocupación a eliminar lógicamente", required = true)
            Long id) {
        return ocupacionUseCase.deleteOcupacion(id)
                .map(v -> Response.noContent().build());
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Obtener una ocupación por ID",
            description = "Recupera la información de una ocupación específica utilizando su ID.")
    @APIResponse(responseCode = "200", description = "Ocupación encontrada",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = OcupacionResponse.class)))
    @APIResponse(responseCode = "404", description = "Ocupación no encontrada",
            content = @Content(mediaType = MediaType.APPLICATION_JSON))
    public Uni<Response> getOcupacionById(
            @PathParam("id")
            @Parameter(description = "ID de la ocupación a recuperar", required = true)
            Long id) {
        return ocupacionUseCase.findOcupacionById(id)
                .map(ocupacion -> Response.ok(ocupacionRestMapper.toResponse(ocupacion)).build());
    }

    @GET
    @Operation(summary = "Listar ocupaciones paginadas",
            description = "Obtiene un listado de ocupaciones con paginación y filtro por nombre.")
    @APIResponse(responseCode = "200", description = "Listado de ocupaciones",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = OcupacionResponse.class)))
    public Uni<Response> getAllOcupaciones(
            @QueryParam("page") @DefaultValue("0")
            @Parameter(description = "Número de página (0-indexed)", required = false)
            Integer page,
            @QueryParam("size") @DefaultValue("10")
            @Parameter(description = "Tamaño de la página", required = false)
            Integer size,
            @QueryParam("nombreFilter") @DefaultValue("")
            @Parameter(description = "Filtro por nombre de ocupación", required = false)
            String nombreFilter) {
        return ocupacionUseCase.findAllOcupaciones(page, size, nombreFilter)
                .map(Response::ok)
                .map(Response.ResponseBuilder::build);
    }

    @GET
    @Path("/all")
    @Operation(summary = "Listar todas las ocupaciones (sin paginar)",
            description = "Obtiene un listado completo de ocupaciones, opcionalmente filtrado por nombre.")
    @APIResponse(responseCode = "200", description = "Listado de todas las ocupaciones",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = OcupacionResponse.class)))
    public Uni<Response> getAllOcupacionesNoPaginated(
            @QueryParam("nombreFilter") @DefaultValue("")
            @Parameter(description = "Filtro por nombre de ocupación", required = false)
            String nombreFilter) {
        return ocupacionUseCase.findAllOcupacionesNoPaginated(nombreFilter)
                .map(Response::ok)
                .map(Response.ResponseBuilder::build);
    }
}
