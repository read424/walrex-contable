package org.walrex.infrastructure.adapter.inbound.rest.resource;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
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
import org.walrex.application.dto.query.PageRequest;
import org.walrex.application.dto.query.ProductAttributeFilter;
import org.walrex.application.dto.request.CreateProductAttributeRequest;
import org.walrex.application.dto.request.UpdateProductAttributeRequest;
import org.walrex.application.dto.response.ErrorResponse;
import org.walrex.application.dto.response.PagedResponse;
import org.walrex.application.dto.response.ProductAttributeResponse;
import org.walrex.application.dto.response.ProductAttributeSelectResponse;
import org.walrex.application.port.input.*;
import org.walrex.domain.exception.DuplicateProductAttributeException;
import org.walrex.domain.exception.ProductAttributeNotFoundException;
import org.walrex.domain.model.ProductAttribute;
import org.walrex.infrastructure.adapter.inbound.mapper.ProductAttributeDtoMapper;

import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Slf4j
@Path("/api/v1/product-attributes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Tag(name = "Atributos de Producto", description = "Gestión de Atributos de Producto")
public class ProductAttributeResource {

    @Inject
    CreateProductAttributeUseCase createUseCase;

    @Inject
    UpdateProductAttributeUseCase updateUseCase;

    @Inject
    DeleteProductAttributeUseCase deleteUseCase;

    @Inject
    GetProductAttributeByIdUseCase getByIdUseCase;

    @Inject
    ListProductAttributeUseCase listUseCase;

    @Inject
    ListAllProductAttributeUseCase listAllUseCase;

    @Inject
    ProductAttributeDtoMapper mapper;

    @POST
    @WithTransaction
    @Operation(summary = "Crear atributo de producto", description = "Crea un nuevo atributo de producto en el sistema")
    @APIResponses({
            @APIResponse(responseCode = "201", description = "Atributo creado exitosamente", content = @Content(
                    mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProductAttributeResponse.class)
            )),
            @APIResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @APIResponse(responseCode = "409", description = "Conflicto: El ID o nombre ya existe"),
            @APIResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public Uni<Response> create(
            @RequestBody(description = "Datos del nuevo atributo", required = true, content = @Content(
                    mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = CreateProductAttributeRequest.class)
            ))
            @Valid CreateProductAttributeRequest request
    ) {
        log.info("REST: Creating product attribute with name: {}", request.name());

        ProductAttribute domain = ProductAttribute.builder()
                .name(request.name())
                .displayType(request.displayType())
                .active(request.active())
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .updatedAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();

        return createUseCase.execute(domain)
                .map(mapper::toResponse)
                .map(response -> Response.created(URI.create("/api/v1/product-attributes/" + response.id()))
                        .entity(response)
                        .build()
                )
                .onFailure()
                .recoverWithItem(this::mapExceptionToResponse);
    }

    @GET
    @Path("/{id}")
    @WithTransaction
    @Operation(summary = "Obtener atributo por ID", description = "Obtiene un atributo de producto por su identificador")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Atributo encontrado", content = @Content(
                    mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProductAttributeResponse.class)
            )),
            @APIResponse(responseCode = "404", description = "Atributo no encontrado"),
            @APIResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public Uni<Response> getById(@PathParam("id") Integer id) {
        log.info("REST: Getting product attribute by id: {}", id);

        return getByIdUseCase.findById(id)
                .map(mapper::toResponse)
                .map(response -> Response.ok(response).build())
                .onFailure()
                .recoverWithItem(this::mapExceptionToResponse);
    }

    @PUT
    @Path("/{id}")
    @WithTransaction
    @Operation(summary = "Actualizar atributo", description = "Actualiza un atributo de producto existente")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Atributo actualizado exitosamente", content = @Content(
                    mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProductAttributeResponse.class)
            )),
            @APIResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @APIResponse(responseCode = "404", description = "Atributo no encontrado"),
            @APIResponse(responseCode = "409", description = "Conflicto: El nombre ya existe en otro atributo"),
            @APIResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public Uni<Response> update(
            @PathParam("id") Integer id,
            @RequestBody(description = "Datos actualizados del atributo", required = true, content = @Content(
                    mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = UpdateProductAttributeRequest.class)
            ))
            @Valid UpdateProductAttributeRequest request
    ) {
        log.info("REST: Updating product attribute id: {}", id);

        ProductAttribute domain = ProductAttribute.builder()
                .id(id)
                .name(request.name())
                .displayType(request.displayType())
                .active(request.active())
                .updatedAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();

        return updateUseCase.execute(id, domain)
                .map(mapper::toResponse)
                .map(response -> Response.ok(response).build())
                .onFailure()
                .recoverWithItem(this::mapExceptionToResponse);
    }

    @DELETE
    @Path("/{id}")
    @WithTransaction
    @Operation(summary = "Eliminar atributo", description = "Elimina lógicamente un atributo de producto (soft delete)")
    @APIResponses({
            @APIResponse(responseCode = "204", description = "Atributo eliminado exitosamente"),
            @APIResponse(responseCode = "404", description = "Atributo no encontrado"),
            @APIResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public Uni<Response> delete(@PathParam("id") Integer id) {
        log.info("REST: Deleting product attribute id: {}", id);

        return deleteUseCase.execute(id)
                .map(deleted -> {
                    if (deleted) {
                        return Response.noContent().build();
                    } else {
                        return Response.status(Response.Status.NOT_FOUND)
                                .entity(new ErrorResponse(404, "Not Found", "ProductAttribute not found with id: " + id))
                                .build();
                    }
                })
                .onFailure()
                .recoverWithItem(this::mapExceptionToResponse);
    }

    @GET
    @WithTransaction
    @Operation(summary = "Listar atributos con paginación", description = "Obtiene un listado paginado de atributos de producto con filtros opcionales")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Lista de atributos obtenida exitosamente", content = @Content(
                    mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PagedResponse.class)
            )),
            @APIResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public Uni<Response> list(
            @QueryParam("page") @DefaultValue("1") int page,
            @QueryParam("size") @DefaultValue("10") int size,
            @QueryParam("sortBy") @DefaultValue("name") String sortBy,
            @QueryParam("sortDirection") @DefaultValue("asc") String sortDirection,
            @QueryParam("search") String search,
            @QueryParam("name") String name,
            @QueryParam("displayType") String displayType,
            @QueryParam("active") String active,
            @QueryParam("includeDeleted") @DefaultValue("0") String includeDeleted
    ) {
        log.info("REST: Listing product attributes with page: {}, size: {}", page, size);

        // Convert from 1-based (API) to 0-based (internal) page numbering
        PageRequest pageRequest = PageRequest.builder()
                .page(page - 1)
                .size(size)
                .sortBy(sortBy)
                .sortDirection(PageRequest.SortDirection.fromString(sortDirection))
                .build();

        ProductAttributeFilter filter = ProductAttributeFilter.builder()
                .search(search)
                .name(name)
                .displayType(displayType != null ? org.walrex.domain.model.AttributeDisplayType.fromString(displayType) : null)
                .active(active)
                .includeDeleted(includeDeleted)
                .build();

        return listUseCase.execute(pageRequest, filter)
                .map(response -> Response.ok(response).build())
                .onFailure()
                .recoverWithItem(this::mapExceptionToResponse);
    }

    @GET
    @Path("/all")
    @WithTransaction
    @Operation(summary = "Listar todos los atributos sin paginación", description = "Obtiene todos los atributos de producto sin paginación (optimizado para selects)")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Lista completa de atributos obtenida exitosamente", content = @Content(
                    mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProductAttributeSelectResponse.class)
            )),
            @APIResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public Uni<Response> listAll(
            @QueryParam("search") String search,
            @QueryParam("name") String name,
            @QueryParam("displayType") String displayType,
            @QueryParam("active") @DefaultValue("1") String active,
            @QueryParam("includeDeleted") @DefaultValue("0") String includeDeleted
    ) {
        log.info("REST: Listing all product attributes without pagination");

        ProductAttributeFilter filter = ProductAttributeFilter.builder()
                .search(search)
                .name(name)
                .displayType(displayType != null ? org.walrex.domain.model.AttributeDisplayType.fromString(displayType) : null)
                .active(active)
                .includeDeleted(includeDeleted)
                .build();

        return listAllUseCase.findAll(filter)
                .map(attributes -> Response.ok(attributes).build())
                .onFailure()
                .recoverWithItem(this::mapExceptionToResponse);
    }

    /**
     * Mapea excepciones de dominio a respuestas HTTP apropiadas.
     */
    private Response mapExceptionToResponse(Throwable t) {
        log.error("REST: Error processing request", t);

        Response.Status status;
        String message = t.getMessage();

        if (t instanceof ProductAttributeNotFoundException) {
            status = Response.Status.NOT_FOUND;
        } else if (t instanceof DuplicateProductAttributeException) {
            status = Response.Status.CONFLICT;
        } else if (t instanceof IllegalArgumentException) {
            status = Response.Status.BAD_REQUEST;
        } else {
            status = Response.Status.INTERNAL_SERVER_ERROR;
            message = "An unexpected error occurred";
        }

        return Response.status(status)
                .entity(new ErrorResponse(status.getStatusCode(), status.getReasonPhrase(), message))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
