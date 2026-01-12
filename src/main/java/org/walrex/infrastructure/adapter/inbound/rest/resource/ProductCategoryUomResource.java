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
import org.walrex.application.dto.query.ProductCategoryUomFilter;
import org.walrex.application.dto.request.CreateProductCategoryUomRequest;
import org.walrex.application.dto.request.UpdateProductCategoryUomRequest;
import org.walrex.application.dto.response.ErrorResponse;
import org.walrex.application.dto.response.PagedResponse;
import org.walrex.application.dto.response.ProductCategoryUomResponse;
import org.walrex.application.dto.response.ProductCategoryUomSelectResponse;
import org.walrex.application.port.input.*;
import org.walrex.domain.exception.DuplicateProductCategoryUomException;
import org.walrex.domain.exception.ProductCategoryUomNotFoundException;
import org.walrex.domain.model.ProductCategoryUom;
import org.walrex.infrastructure.adapter.inbound.mapper.ProductCategoryUomDtoMapper;

import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Slf4j
@Path("/api/v1/product-category-uoms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Tag(name = "Categoría de Unidad de Medida", description = "Gestión de Categorías de Unidades de Medida")
public class ProductCategoryUomResource {

    @Inject
    CreateProductCategoryUomUseCase createUseCase;

    @Inject
    UpdateProductCategoryUomUseCase updateUseCase;

    @Inject
    DeleteProductCategoryUomUseCase deleteUseCase;

    @Inject
    GetProductCategoryUomByIdUseCase getByIdUseCase;

    @Inject
    ListProductCategoryUomUseCase listUseCase;

    @Inject
    ListAllProductCategoryUomUseCase listAllUseCase;

    @Inject
    ProductCategoryUomDtoMapper mapper;

    @POST
    @WithTransaction
    @Operation(summary = "Crear categoría de unidad de medida", description = "Crea una nueva categoría de unidad de medida en el sistema")
    @APIResponses({
            @APIResponse(responseCode = "201", description = "Categoría creada exitosamente", content = @Content(
                    mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProductCategoryUomResponse.class)
            )),
            @APIResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @APIResponse(responseCode = "409", description = "Conflicto: El código o nombre ya existe"),
            @APIResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public Uni<Response> create(
            @RequestBody(description = "Datos de la nueva categoría", required = true, content = @Content(
                    mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = CreateProductCategoryUomRequest.class)
            ))
            @Valid CreateProductCategoryUomRequest request
    ) {
        log.info("REST: Creating product category uom with code: {}", request.code());

        ProductCategoryUom domain = ProductCategoryUom.builder()
                .code(request.code())
                .name(request.name())
                .description(request.description())
                .active(request.active())
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .updatedAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();

        return createUseCase.execute(domain)
                .map(mapper::toResponse)
                .map(response -> Response.created(URI.create("/api/v1/product-category-uoms/" + response.id()))
                        .entity(response)
                        .build()
                )
                .onFailure()
                .recoverWithItem(this::mapExceptionToResponse);
    }

    @GET
    @Path("/{id}")
    @WithTransaction
    @Operation(summary = "Obtener categoría por ID", description = "Obtiene una categoría de unidad de medida por su identificador")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Categoría encontrada", content = @Content(
                    mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProductCategoryUomResponse.class)
            )),
            @APIResponse(responseCode = "404", description = "Categoría no encontrada"),
            @APIResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public Uni<Response> getById(@PathParam("id") Integer id) {
        log.info("REST: Getting product category uom by id: {}", id);

        return getByIdUseCase.findById(id)
                .map(mapper::toResponse)
                .map(response -> Response.ok(response).build())
                .onFailure()
                .recoverWithItem(this::mapExceptionToResponse);
    }

    @PUT
    @Path("/{id}")
    @WithTransaction
    @Operation(summary = "Actualizar categoría", description = "Actualiza una categoría de unidad de medida existente")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Categoría actualizada exitosamente", content = @Content(
                    mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProductCategoryUomResponse.class)
            )),
            @APIResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @APIResponse(responseCode = "404", description = "Categoría no encontrada"),
            @APIResponse(responseCode = "409", description = "Conflicto: El código o nombre ya existe en otra categoría"),
            @APIResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public Uni<Response> update(
            @PathParam("id") Integer id,
            @RequestBody(description = "Datos actualizados de la categoría", required = true, content = @Content(
                    mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = UpdateProductCategoryUomRequest.class)
            ))
            @Valid UpdateProductCategoryUomRequest request
    ) {
        log.info("REST: Updating product category uom id: {}", id);

        ProductCategoryUom domain = ProductCategoryUom.builder()
                .id(id)
                .code(request.code())
                .name(request.name())
                .description(request.description())
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
    @Operation(summary = "Eliminar categoría", description = "Elimina lógicamente una categoría de unidad de medida (soft delete)")
    @APIResponses({
            @APIResponse(responseCode = "204", description = "Categoría eliminada exitosamente"),
            @APIResponse(responseCode = "404", description = "Categoría no encontrada"),
            @APIResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public Uni<Response> delete(@PathParam("id") Integer id) {
        log.info("REST: Deleting product category uom id: {}", id);

        return deleteUseCase.execute(id)
                .map(deleted -> {
                    if (deleted) {
                        return Response.noContent().build();
                    } else {
                        return Response.status(Response.Status.NOT_FOUND)
                                .entity(new ErrorResponse(404, "Not Found", "ProductCategoryUom not found with id: " + id))
                                .build();
                    }
                })
                .onFailure()
                .recoverWithItem(this::mapExceptionToResponse);
    }

    @GET
    @WithTransaction
    @Operation(summary = "Listar categorías con paginación", description = "Obtiene un listado paginado de categorías de unidades de medida con filtros opcionales")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Lista de categorías obtenida exitosamente", content = @Content(
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
            @QueryParam("code") String code,
            @QueryParam("name") String name,
            @QueryParam("active") String active,
            @QueryParam("includeDeleted") @DefaultValue("0") String includeDeleted
    ) {
        log.info("REST: Listing product category uoms with page: {}, size: {}", page, size);

        // Convert from 1-based (API) to 0-based (internal) page numbering
        PageRequest pageRequest = PageRequest.builder()
                .page(page - 1)
                .size(size)
                .sortBy(sortBy)
                .sortDirection(PageRequest.SortDirection.fromString(sortDirection))
                .build();

        ProductCategoryUomFilter filter = ProductCategoryUomFilter.builder()
                .search(search)
                .code(code)
                .name(name)
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
    @Operation(summary = "Listar todas las categorías sin paginación", description = "Obtiene todas las categorías de unidades de medida sin paginación (optimizado para selects)")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Lista completa de categorías obtenida exitosamente", content = @Content(
                    mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProductCategoryUomSelectResponse.class)
            )),
            @APIResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public Uni<Response> listAll(
            @QueryParam("search") String search,
            @QueryParam("code") String code,
            @QueryParam("name") String name,
            @QueryParam("active") @DefaultValue("1") String active,
            @QueryParam("includeDeleted") @DefaultValue("0") String includeDeleted
    ) {
        log.info("REST: Listing all product category uoms without pagination");

        ProductCategoryUomFilter filter = ProductCategoryUomFilter.builder()
                .search(search)
                .code(code)
                .name(name)
                .active(active)
                .includeDeleted(includeDeleted)
                .build();

        return listAllUseCase.findAll(filter)
                .map(categories -> Response.ok(categories).build())
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

        if (t instanceof ProductCategoryUomNotFoundException) {
            status = Response.Status.NOT_FOUND;
        } else if (t instanceof DuplicateProductCategoryUomException) {
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
