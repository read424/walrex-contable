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
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.walrex.application.dto.request.CreateCategoryProductRequest;
import org.walrex.application.dto.response.CategoryProductResponse;
import org.walrex.application.dto.response.CategoryProductSelectResponse;
import org.walrex.application.dto.response.ErrorResponse;
import org.walrex.application.port.input.CreateCategoryProductUseCase;
import org.walrex.application.port.input.ListCategoryProductUseCase;
import org.walrex.domain.exception.CategoryProductNotFoundException;
import org.walrex.domain.exception.DuplicateCategoryProductException;
import org.walrex.infrastructure.adapter.inbound.mapper.CategoryProductMapper;

import java.net.URI;
import java.util.List;

@Path("/api/v1/categoryproduct")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Tag(name = "Categoria de Producto", description = "Gestión de Categoria de Productos")
public class CategoryProductResource {

    @Inject
    CreateCategoryProductUseCase createCategoryProductUseCase;

    @Inject
    ListCategoryProductUseCase listCategoryProductUseCase;

    @Inject
    CategoryProductMapper categoryProductMapper;

    @POST
    @WithTransaction
    @Operation(summary = "Crear categoria producto", description = "")
    @APIResponses(
            {
                    @APIResponse(responseCode = "201", description = "Categoria Producto creado exitosamente", content = @Content(
                            mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = CategoryProductResponse.class)
                    )),
                    @APIResponse(responseCode = "400", description = "Datos de entrada inválidos"),
                    @APIResponse(responseCode = "409", description = "Conflicto: El nombre ya existe"),
                    @APIResponse(responseCode = "500", description = "Error interno del servidor")
            }
    )
    public Uni<Response> create(
            @RequestBody(description = "Datos de la nueva Categoria de Producto", required = true, content = @Content(
                    mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = CreateCategoryProductRequest.class)
            ))
            @Valid CreateCategoryProductRequest request
    ){
        return createCategoryProductUseCase.agregarCategory(categoryProductMapper.toDomain(request))
                .map(categoryProductMapper::toResponse)
                .map(response -> Response.created(URI.create("/api/v1/categoryproduct"+response.getId()))
                        .entity(response)
                        .build()
                ).onFailure()
                .recoverWithItem(this::mapExceptionToResponse);
    }

    @GET
    @WithTransaction
    @Operation(summary = "Listar categorías de producto", description = "Obtiene una lista de categorías filtradas por parentId. Si parentId es null, devuelve las categorías raíz.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Lista de categorías obtenida exitosamente", content = @Content(
                    mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = CategoryProductSelectResponse.class)
            )),
            @APIResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public Uni<Response> list(
            @Parameter(description = "ID de la categoría padre. Si es null, devuelve las categorías raíz", required = false)
            @QueryParam("parentId") Integer parentId
    ) {
        return listCategoryProductUseCase.listCategoriesByParentId(parentId)
                .map(categories -> {
                    List<CategoryProductSelectResponse> responses = categories.stream()
                            .map(categoryProductMapper::toSelectResponse)
                            .toList();
                    return Response.ok(responses).build();
                })
                .onFailure()
                .recoverWithItem(this::mapExceptionToResponse);
    }

    private Response mapExceptionToResponse(Throwable t) {
        Response.Status status;

        if (t instanceof CategoryProductNotFoundException) {
            status = Response.Status.NOT_FOUND;
        } else if (t instanceof DuplicateCategoryProductException) {
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
