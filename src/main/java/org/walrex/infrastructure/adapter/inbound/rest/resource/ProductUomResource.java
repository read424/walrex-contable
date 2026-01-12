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
import org.walrex.application.dto.query.ProductUomFilter;
import org.walrex.application.dto.request.CreateProductUomRequest;
import org.walrex.application.dto.request.UpdateProductUomRequest;
import org.walrex.application.dto.response.ErrorResponse;
import org.walrex.application.dto.response.PagedResponse;
import org.walrex.application.dto.response.ProductUomResponse;
import org.walrex.application.dto.response.ProductUomSelectResponse;
import org.walrex.application.port.input.*;
import org.walrex.domain.exception.DuplicateProductUomException;
import org.walrex.domain.exception.ProductCategoryUomNotFoundException;
import org.walrex.domain.exception.ProductUomNotFoundException;
import org.walrex.domain.model.ProductUom;
import org.walrex.infrastructure.adapter.inbound.mapper.ProductUomDtoMapper;

import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Slf4j
@Path("/api/v1/product-uoms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Tag(name = "Unidad de Medida de Producto", description = "Gestión de Unidades de Medida de Productos")
public class ProductUomResource {

    @Inject
    CreateProductUomUseCase createUseCase;

    @Inject
    UpdateProductUomUseCase updateUseCase;

    @Inject
    DeleteProductUomUseCase deleteUseCase;

    @Inject
    GetProductUomByIdUseCase getByIdUseCase;

    @Inject
    ListProductUomUseCase listUseCase;

    @Inject
    ListAllProductUomUseCase listAllUseCase;

    @Inject
    ProductUomDtoMapper mapper;

    @POST
    @WithTransaction
    @Operation(summary = "Crear unidad de medida", description = "Crea una nueva unidad de medida de producto en el sistema")
    @APIResponses({
            @APIResponse(responseCode = "201", description = "Unidad creada exitosamente", content = @Content(
                    mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProductUomResponse.class)
            )),
            @APIResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @APIResponse(responseCode = "404", description = "Categoría no encontrada"),
            @APIResponse(responseCode = "409", description = "Conflicto: El código ya existe"),
            @APIResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public Uni<Response> create(
            @RequestBody(description = "Datos de la nueva unidad de medida", required = true, content = @Content(
                    mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = CreateProductUomRequest.class)
            ))
            @Valid CreateProductUomRequest request
    ) {
        log.info("REST: Creating product UOM with code: {}", request.codeUom());

        ProductUom domain = ProductUom.builder()
                .codeUom(request.codeUom())
                .nameUom(request.nameUom())
                .categoryId(request.categoryId())
                .factor(request.factor())
                .roundingPrecision(request.roundingPrecision())
                .active(request.active())
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .updatedAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();

        return createUseCase.execute(domain)
                .map(mapper::toResponse)
                .map(response -> Response.created(URI.create("/api/v1/product-uoms/" + response.id()))
                        .entity(response)
                        .build()
                )
                .onFailure()
                .recoverWithItem(this::mapExceptionToResponse);
    }

    @GET
    @Path("/{id}")
    @WithTransaction
    @Operation(summary = "Obtener unidad de medida por ID", description = "Obtiene una unidad de medida de producto por su identificador")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Unidad encontrada", content = @Content(
                    mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProductUomResponse.class)
            )),
            @APIResponse(responseCode = "404", description = "Unidad no encontrada"),
            @APIResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public Uni<Response> getById(@PathParam("id") Integer id) {
        log.info("REST: Getting product UOM by id: {}", id);

        return getByIdUseCase.findById(id)
                .map(mapper::toResponse)
                .map(response -> Response.ok(response).build())
                .onFailure()
                .recoverWithItem(this::mapExceptionToResponse);
    }

    @PUT
    @Path("/{id}")
    @WithTransaction
    @Operation(summary = "Actualizar unidad de medida", description = "Actualiza una unidad de medida de producto existente")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Unidad actualizada exitosamente", content = @Content(
                    mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProductUomResponse.class)
            )),
            @APIResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @APIResponse(responseCode = "404", description = "Unidad o categoría no encontrada"),
            @APIResponse(responseCode = "409", description = "Conflicto: El código ya existe en otra unidad"),
            @APIResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public Uni<Response> update(
            @PathParam("id") Integer id,
            @RequestBody(description = "Datos actualizados de la unidad de medida", required = true, content = @Content(
                    mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = UpdateProductUomRequest.class)
            ))
            @Valid UpdateProductUomRequest request
    ) {
        log.info("REST: Updating product UOM id: {}", id);

        ProductUom domain = ProductUom.builder()
                .id(id)
                .codeUom(request.codeUom())
                .nameUom(request.nameUom())
                .categoryId(request.categoryId())
                .factor(request.factor())
                .roundingPrecision(request.roundingPrecision())
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
    @Operation(summary = "Eliminar unidad de medida", description = "Elimina lógicamente una unidad de medida de producto (soft delete)")
    @APIResponses({
            @APIResponse(responseCode = "204", description = "Unidad eliminada exitosamente"),
            @APIResponse(responseCode = "404", description = "Unidad no encontrada"),
            @APIResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public Uni<Response> delete(@PathParam("id") Integer id) {
        log.info("REST: Deleting product UOM id: {}", id);

        return deleteUseCase.execute(id)
                .map(deleted -> {
                    if (deleted) {
                        return Response.noContent().build();
                    } else {
                        return Response.status(Response.Status.NOT_FOUND)
                                .entity(new ErrorResponse(404, "Not Found", "ProductUom not found with id: " + id))
                                .build();
                    }
                })
                .onFailure()
                .recoverWithItem(this::mapExceptionToResponse);
    }

    @GET
    @WithTransaction
    @Operation(summary = "Listar unidades de medida con paginación", description = "Obtiene un listado paginado de unidades de medida con filtros opcionales")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Lista de unidades obtenida exitosamente", content = @Content(
                    mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PagedResponse.class)
            )),
            @APIResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public Uni<Response> list(
            @QueryParam("page") @DefaultValue("1") int page,
            @QueryParam("size") @DefaultValue("10") int size,
            @QueryParam("sortBy") @DefaultValue("nameUom") String sortBy,
            @QueryParam("sortDirection") @DefaultValue("asc") String sortDirection,
            @QueryParam("search") String search,
            @QueryParam("codeUom") String codeUom,
            @QueryParam("nameUom") String nameUom,
            @QueryParam("categoryId") Integer categoryId,
            @QueryParam("active") String active,
            @QueryParam("includeDeleted") @DefaultValue("0") String includeDeleted
    ) {
        log.info("REST: Listing product UOMs with page: {}, size: {}", page, size);

        // Convert from 1-based (API) to 0-based (internal) page numbering
        PageRequest pageRequest = PageRequest.builder()
                .page(page - 1)
                .size(size)
                .sortBy(sortBy)
                .sortDirection(PageRequest.SortDirection.fromString(sortDirection))
                .build();

        ProductUomFilter filter = ProductUomFilter.builder()
                .search(search)
                .codeUom(codeUom)
                .nameUom(nameUom)
                .categoryId(categoryId)
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
    @Operation(summary = "Listar todas las unidades de medida sin paginación", description = "Obtiene todas las unidades de medida sin paginación (optimizado para selects)")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Lista completa de unidades obtenida exitosamente", content = @Content(
                    mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProductUomSelectResponse.class)
            )),
            @APIResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public Uni<Response> listAll(
            @QueryParam("search") String search,
            @QueryParam("codeUom") String codeUom,
            @QueryParam("nameUom") String nameUom,
            @QueryParam("categoryId") Integer categoryId,
            @QueryParam("active") @DefaultValue("1") String active,
            @QueryParam("includeDeleted") @DefaultValue("0") String includeDeleted
    ) {
        log.info("REST: Listing all product UOMs without pagination");

        ProductUomFilter filter = ProductUomFilter.builder()
                .search(search)
                .codeUom(codeUom)
                .nameUom(nameUom)
                .categoryId(categoryId)
                .active(active)
                .includeDeleted(includeDeleted)
                .build();

        return listAllUseCase.findAll(filter)
                .map(uoms -> Response.ok(uoms).build())
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

        if (t instanceof ProductUomNotFoundException) {
            status = Response.Status.NOT_FOUND;
        } else if (t instanceof ProductCategoryUomNotFoundException) {
            status = Response.Status.NOT_FOUND;
        } else if (t instanceof DuplicateProductUomException) {
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
