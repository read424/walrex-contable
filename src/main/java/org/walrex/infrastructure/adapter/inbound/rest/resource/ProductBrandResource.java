package org.walrex.infrastructure.adapter.inbound.rest.resource;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
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
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.walrex.application.dto.request.CreateProductBrandRequest;
import org.walrex.application.dto.response.ErrorResponse;
import org.walrex.application.dto.response.ProductBrandResponse;
import org.walrex.application.port.input.CreateProductBrandUseCase;
import org.walrex.application.port.input.ListAllProductBrandUseCase;
import org.walrex.application.port.input.ListProductBrandUseCase;
import org.walrex.domain.exception.DuplicateProductBrandException;
import org.walrex.infrastructure.adapter.inbound.mapper.ProductBrandMapper;

import java.net.URI;
import java.util.List;

@Path("/api/v1/product-brands")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Tag(name = "Marca de Producto", description = "Gestión de Marcas de Productos")
public class ProductBrandResource {

    @Inject
    CreateProductBrandUseCase createProductBrandUseCase;

    @Inject
    ListProductBrandUseCase listProductBrandUseCase;

    @Inject
    ListAllProductBrandUseCase listAllProductBrandUseCase;

    @Inject
    ProductBrandMapper productBrandMapper;

    @POST
    @WithTransaction
    @Operation(summary = "Crear marca de producto", description = "Crea una nueva marca de producto en el sistema")
    @APIResponses(
            {
                    @APIResponse(responseCode = "201", description = "Marca de producto creada exitosamente", content = @Content(
                            mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProductBrandResponse.class)
                    )),
                    @APIResponse(responseCode = "400", description = "Datos de entrada inválidos"),
                    @APIResponse(responseCode = "409", description = "Conflicto: El nombre ya existe"),
                    @APIResponse(responseCode = "500", description = "Error interno del servidor")
            }
    )
    public Uni<Response> create(
            @RequestBody(description = "Datos de la nueva marca de producto", required = true, content = @Content(
                    mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = CreateProductBrandRequest.class)
            ))
            @Valid CreateProductBrandRequest request
    ){
        return createProductBrandUseCase.createProductBrand(productBrandMapper.toDomain(request))
                .map(productBrandMapper::toResponse)
                .map(response -> Response.created(URI.create("/api/v1/product-brands/" + response.getId()))
                        .entity(response)
                        .build()
                ).onFailure()
                .recoverWithItem(this::mapExceptionToResponse);
    }

    @GET
    @WithTransaction
    @Operation(summary = "Listar marcas de producto", description = "Obtiene el listado completo de marcas de producto")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Lista de marcas obtenida exitosamente", content = @Content(
                    mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProductBrandResponse.class)
            )),
            @APIResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public Uni<Response> list() {
        return listProductBrandUseCase.listProductBrands()
                .map(brands -> {
                    List<ProductBrandResponse> responses = brands.stream()
                            .map(productBrandMapper::toResponse)
                            .toList();
                    return Response.ok(responses).build();
                })
                .onFailure()
                .recoverWithItem(this::mapExceptionToResponse);
    }

    @GET
    @Path("/all")
    @WithTransaction
    @Operation(summary = "Listar todas las marcas sin paginación", description = "Obtiene todas las marcas de producto sin paginación (optimizado para selects con caché de 15 minutos)")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Lista completa de marcas obtenida exitosamente", content = @Content(
                    mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProductBrandResponse.class)
            )),
            @APIResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public Uni<Response> listAll() {
        return listAllProductBrandUseCase.findAll()
                .map(brands -> Response.ok(brands).build())
                .onFailure()
                .recoverWithItem(this::mapExceptionToResponse);
    }

    private Response mapExceptionToResponse(Throwable t) {
        Response.Status status;

        if (t instanceof DuplicateProductBrandException) {
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
