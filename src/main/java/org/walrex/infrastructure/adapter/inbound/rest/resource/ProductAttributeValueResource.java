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
import org.walrex.application.dto.query.ProductAttributeValueFilter;
import org.walrex.application.dto.request.CreateProductAttributeValueRequest;
import org.walrex.application.dto.request.UpdateProductAttributeValueRequest;
import org.walrex.application.dto.response.ErrorResponse;
import org.walrex.application.dto.response.PagedResponse;
import org.walrex.application.dto.response.ProductAttributeValueResponse;
import org.walrex.application.dto.response.ProductAttributeValueSelectResponse;
import org.walrex.application.port.input.*;
import org.walrex.domain.exception.DuplicateProductAttributeValueException;
import org.walrex.domain.exception.ProductAttributeNotFoundException;
import org.walrex.domain.exception.ProductAttributeValueNotFoundException;
import org.walrex.domain.model.ProductAttributeValue;
import org.walrex.infrastructure.adapter.inbound.mapper.ProductAttributeValueDtoMapper;

import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Slf4j
@Path("/api/v1/product-attribute-values")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Tag(name = "Valores de Atributos de Producto", description = "Gestión de Valores de Atributos de Producto")
public class ProductAttributeValueResource {

    @Inject
    CreateProductAttributeValueUseCase createUseCase;

    @Inject
    UpdateProductAttributeValueUseCase updateUseCase;

    @Inject
    DeleteProductAttributeValueUseCase deleteUseCase;

    @Inject
    GetProductAttributeValueByIdUseCase getByIdUseCase;

    @Inject
    ListProductAttributeValueUseCase listUseCase;

    @Inject
    ListAllProductAttributeValueUseCase listAllUseCase;

    @Inject
    ProductAttributeValueDtoMapper mapper;

    @POST
    @WithTransaction
    @Operation(summary = "Crear valor de atributo", description = "Crea un nuevo valor de atributo de producto en el sistema")
    @APIResponses({
            @APIResponse(responseCode = "201", description = "Valor de atributo creado exitosamente", content = @Content(
                    mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProductAttributeValueResponse.class)
            )),
            @APIResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @APIResponse(responseCode = "404", description = "Atributo padre no encontrado"),
            @APIResponse(responseCode = "409", description = "Conflicto: El ID o combinación (attributeId, name) ya existe"),
            @APIResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public Uni<Response> create(
            @RequestBody(description = "Datos del nuevo valor de atributo", required = true, content = @Content(
                    mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = CreateProductAttributeValueRequest.class)
            ))
            @Valid CreateProductAttributeValueRequest request
    ) {
        log.info("REST: Creating product attribute value with name: {}", request.name());

        ProductAttributeValue domain = ProductAttributeValue.builder()
                .attributeId(request.attributeId())
                .name(request.name())
                .htmlColor(request.htmlColor())
                .sequence(request.sequence())
                .active(request.active())
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .updatedAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();

        return createUseCase.execute(domain)
                .map(mapper::toResponse)
                .map(response -> Response.created(URI.create("/api/v1/product-attribute-values/" + response.id()))
                        .entity(response)
                        .build()
                )
                .onFailure()
                .recoverWithItem(this::mapExceptionToResponse);
    }

    @GET
    @Path("/{id}")
    @WithTransaction
    @Operation(summary = "Obtener valor de atributo por ID", description = "Obtiene un valor de atributo de producto por su identificador")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Valor de atributo encontrado", content = @Content(
                    mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProductAttributeValueResponse.class)
            )),
            @APIResponse(responseCode = "404", description = "Valor de atributo no encontrado"),
            @APIResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public Uni<Response> getById(@PathParam("id") Integer id) {
        log.info("REST: Getting product attribute value by id: {}", id);

        return getByIdUseCase.findById(id)
                .map(mapper::toResponse)
                .map(response -> Response.ok(response).build())
                .onFailure()
                .recoverWithItem(this::mapExceptionToResponse);
    }

    @PUT
    @Path("/{id}")
    @WithTransaction
    @Operation(summary = "Actualizar valor de atributo", description = "Actualiza un valor de atributo de producto existente")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Valor de atributo actualizado exitosamente", content = @Content(
                    mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProductAttributeValueResponse.class)
            )),
            @APIResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @APIResponse(responseCode = "404", description = "Valor de atributo no encontrado"),
            @APIResponse(responseCode = "409", description = "Conflicto: La combinación (attributeId, name) ya existe en otro valor"),
            @APIResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public Uni<Response> update(
            @PathParam("id") Integer id,
            @RequestBody(description = "Datos actualizados del valor de atributo", required = true, content = @Content(
                    mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = UpdateProductAttributeValueRequest.class)
            ))
            @Valid UpdateProductAttributeValueRequest request
    ) {
        log.info("REST: Updating product attribute value id: {}", id);

        // Primero obtener el valor existente para mantener el attributeId
        return getByIdUseCase.findById(id)
                .onItem().transformToUni(existingValue -> {
                    ProductAttributeValue domain = ProductAttributeValue.builder()
                            .id(id)
                            .attributeId(existingValue.getAttributeId()) // Mantener el attributeId existente (no se puede cambiar)
                            .name(request.name())
                            .htmlColor(request.htmlColor())
                            .sequence(request.sequence())
                            .active(request.active())
                            .updatedAt(OffsetDateTime.now(ZoneOffset.UTC))
                            .build();

                    return updateUseCase.execute(id, domain);
                })
                .map(mapper::toResponse)
                .map(response -> Response.ok(response).build())
                .onFailure()
                .recoverWithItem(this::mapExceptionToResponse);
    }

    @DELETE
    @Path("/{id}")
    @WithTransaction
    @Operation(summary = "Eliminar valor de atributo", description = "Elimina lógicamente un valor de atributo de producto (soft delete)")
    @APIResponses({
            @APIResponse(responseCode = "204", description = "Valor de atributo eliminado exitosamente"),
            @APIResponse(responseCode = "404", description = "Valor de atributo no encontrado"),
            @APIResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public Uni<Response> delete(@PathParam("id") Integer id) {
        log.info("REST: Deleting product attribute value id: {}", id);

        return deleteUseCase.execute(id)
                .map(deleted -> {
                    if (deleted) {
                        return Response.noContent().build();
                    } else {
                        return Response.status(Response.Status.NOT_FOUND)
                                .entity(new ErrorResponse(404, "Not Found", "ProductAttributeValue not found with id: " + id))
                                .build();
                    }
                })
                .onFailure()
                .recoverWithItem(this::mapExceptionToResponse);
    }

    @GET
    @WithTransaction
    @Operation(summary = "Listar valores de atributos con paginación", description = "Obtiene un listado paginado de valores de atributos de producto con filtros opcionales")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Lista de valores de atributos obtenida exitosamente", content = @Content(
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
            @QueryParam("attributeId") Integer attributeId,
            @QueryParam("active") String active,
            @QueryParam("includeDeleted") @DefaultValue("0") String includeDeleted
    ) {
        log.info("REST: Listing product attribute values with page: {}, size: {}", page, size);

        // Convert from 1-based (API) to 0-based (internal) page numbering
        PageRequest pageRequest = PageRequest.builder()
                .page(page - 1)
                .size(size)
                .sortBy(sortBy)
                .sortDirection(PageRequest.SortDirection.fromString(sortDirection))
                .build();

        ProductAttributeValueFilter filter = ProductAttributeValueFilter.builder()
                .search(search)
                .name(name)
                .attributeId(attributeId)
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
    @Operation(summary = "Listar todos los valores de atributos sin paginación", description = "Obtiene todos los valores de atributos de producto sin paginación (optimizado para selects)")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Lista completa de valores de atributos obtenida exitosamente", content = @Content(
                    mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProductAttributeValueSelectResponse.class)
            )),
            @APIResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public Uni<Response> listAll(
            @QueryParam("search") String search,
            @QueryParam("name") String name,
            @QueryParam("attributeId") Integer attributeId,
            @QueryParam("active") @DefaultValue("1") String active,
            @QueryParam("includeDeleted") @DefaultValue("0") String includeDeleted
    ) {
        log.info("REST: Listing all product attribute values");

        ProductAttributeValueFilter filter = ProductAttributeValueFilter.builder()
                .search(search)
                .name(name)
                .attributeId(attributeId)
                .active(active)
                .includeDeleted(includeDeleted)
                .build();

        return listAllUseCase.findAll(filter)
                .map(response -> Response.ok(response).build())
                .onFailure()
                .recoverWithItem(this::mapExceptionToResponse);
    }

    @GET
    @Path("/by-attribute/{attributeId}")
    @WithTransaction
    @Operation(summary = "Listar valores por atributo", description = "Obtiene todos los valores de un atributo específico (útil para formularios)")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Lista de valores del atributo obtenida exitosamente", content = @Content(
                    mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProductAttributeValueSelectResponse.class)
            )),
            @APIResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public Uni<Response> listByAttribute(
            @PathParam("attributeId") Integer attributeId,
            @QueryParam("active") @DefaultValue("1") String active
    ) {
        log.info("REST: Listing product attribute values for attributeId: {}", attributeId);

        ProductAttributeValueFilter filter = ProductAttributeValueFilter.builder()
                .attributeId(attributeId)
                .active(active)
                .includeDeleted("0")
                .build();

        return listAllUseCase.findAll(filter)
                .map(response -> Response.ok(response).build())
                .onFailure()
                .recoverWithItem(this::mapExceptionToResponse);
    }

    // ==================== Exception Handling ====================

    private Response mapExceptionToResponse(Throwable throwable) {
        log.error("Error processing request", throwable);

        if (throwable instanceof ProductAttributeValueNotFoundException) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse(404, "Not Found", throwable.getMessage()))
                    .build();
        } else if (throwable instanceof ProductAttributeNotFoundException) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse(404, "Not Found", throwable.getMessage()))
                    .build();
        } else if (throwable instanceof DuplicateProductAttributeValueException) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(new ErrorResponse(409, "Conflict", throwable.getMessage()))
                    .build();
        } else if (throwable instanceof IllegalArgumentException) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(400, "Bad Request", throwable.getMessage()))
                    .build();
        } else {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse(500, "Internal Server Error", "An unexpected error occurred"))
                    .build();
        }
    }
}
