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
import org.walrex.application.dto.query.ProductTemplateFilter;
import org.walrex.application.dto.request.CreateProductTemplateRequest;
import org.walrex.application.dto.request.UpdateProductTemplateRequest;
import org.walrex.application.dto.response.ErrorResponse;
import org.walrex.application.dto.response.PagedResponse;
import org.walrex.application.dto.response.ProductTemplateResponse;
import org.walrex.application.dto.response.ProductTemplateSelectResponse;
import org.walrex.application.port.input.*;
import org.walrex.domain.exception.*;
import org.walrex.domain.model.ProductTemplate;
import org.walrex.domain.model.ProductType;
import org.walrex.infrastructure.adapter.inbound.mapper.ProductTemplateDtoMapper;

import java.net.URI;

@Slf4j
@Path("/api/v1/products")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Tag(name = "Productos", description = "Gestión de Productos y Servicios (con o sin variantes configurables)")
public class ProductTemplateResource {

    @Inject
    CreateProductTemplateUseCase createUseCase;

    @Inject
    CreateProductTemplateWithVariantsUseCase createWithVariantsUseCase;

    @Inject
    UpdateProductTemplateUseCase updateUseCase;

    @Inject
    DeleteProductTemplateUseCase deleteUseCase;

    @Inject
    GetProductTemplateByIdUseCase getByIdUseCase;

    @Inject
    ListProductTemplateUseCase listUseCase;

    @Inject
    ListAllProductTemplateUseCase listAllUseCase;

    @Inject
    ProductTemplateDtoMapper mapper;

    @POST
    @WithTransaction
    @Operation(
            summary = "Crear plantilla de producto",
            description = "Crea una nueva plantilla de producto o servicio en el sistema. " +
                    "Soporta productos simples (SERVICE, STORABLE, CONSUMABLE sin variantes configurables) " +
                    "y productos con variantes configurables (STORABLE/CONSUMABLE con atributos). " +
                    "Para productos con variantes, incluye los campos attributeIds y variants en el request."
    )
    @APIResponses({
            @APIResponse(responseCode = "201", description = "Plantilla creada exitosamente", content = @Content(
                    mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProductTemplateResponse.class)
            )),
            @APIResponse(responseCode = "400", description = "Datos de entrada inválidos o combinaciones de variantes duplicadas"),
            @APIResponse(responseCode = "404", description = "Categoría, marca, UOM, moneda, atributo o valor de atributo no encontrado"),
            @APIResponse(responseCode = "409", description = "Conflicto: La referencia interna ya existe"),
            @APIResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public Uni<Response> create(
            @RequestBody(description = "Datos de la nueva plantilla de producto", required = true, content = @Content(
                    mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = CreateProductTemplateRequest.class)
            ))
            @Valid CreateProductTemplateRequest request
    ) {
        // Detectar si tiene variantes configurables
        boolean hasConfigurableVariants = request.attributeIds() != null
                && !request.attributeIds().isEmpty()
                && request.variants() != null
                && !request.variants().isEmpty();

        if (hasConfigurableVariants) {
            log.info("REST: Creating product template with configurable variants: {} ({} variants)",
                    request.name(), request.variants().size());

            // Delegar al servicio de variantes configurables
            return createWithVariantsUseCase.execute(request)
                    .map(mapper::toResponse)
                    .map(response -> Response.created(URI.create("/api/v1/products/" + response.id()))
                            .entity(response)
                            .build()
                    )
                    .onFailure()
                    .recoverWithItem(this::mapExceptionToResponse);
        } else {
            log.info("REST: Creating simple product template: {}", request.name());

            // Delegar al servicio estándar (productos simples o con variante por defecto)
            ProductTemplate domain = mapper.toDomain(request);

            return createUseCase.execute(domain)
                    .map(mapper::toResponse)
                    .map(response -> Response.created(URI.create("/api/v1/products/" + response.id()))
                            .entity(response)
                            .build()
                    )
                    .onFailure()
                    .recoverWithItem(this::mapExceptionToResponse);
        }
    }

    @GET
    @Path("/{id}")
    @WithTransaction
    @Operation(summary = "Obtener plantilla de producto por ID", description = "Obtiene una plantilla de producto por su identificador")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Plantilla encontrada", content = @Content(
                    mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProductTemplateResponse.class)
            )),
            @APIResponse(responseCode = "404", description = "Plantilla no encontrada"),
            @APIResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public Uni<Response> getById(@PathParam("id") Integer id) {
        log.info("REST: Getting product template by id: {}", id);

        return getByIdUseCase.findById(id)
                .map(mapper::toResponse)
                .map(response -> Response.ok(response).build())
                .onFailure()
                .recoverWithItem(this::mapExceptionToResponse);
    }

    @PUT
    @Path("/{id}")
    @WithTransaction
    @Operation(summary = "Actualizar plantilla de producto", description = "Actualiza una plantilla de producto existente")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Plantilla actualizada exitosamente", content = @Content(
                    mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProductTemplateResponse.class)
            )),
            @APIResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @APIResponse(responseCode = "404", description = "Plantilla, categoría, marca, UOM o moneda no encontrada"),
            @APIResponse(responseCode = "409", description = "Conflicto: La referencia interna ya existe en otra plantilla"),
            @APIResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public Uni<Response> update(
            @PathParam("id") Integer id,
            @RequestBody(description = "Datos actualizados de la plantilla de producto", required = true, content = @Content(
                    mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = UpdateProductTemplateRequest.class)
            ))
            @Valid UpdateProductTemplateRequest request
    ) {
        log.info("REST: Updating product template id: {}", id);

        ProductTemplate domain = mapper.toDomain(request);
        domain.setId(id);

        return updateUseCase.execute(id, domain)
                .map(mapper::toResponse)
                .map(response -> Response.ok(response).build())
                .onFailure()
                .recoverWithItem(this::mapExceptionToResponse);
    }

    @DELETE
    @Path("/{id}")
    @WithTransaction
    @Operation(summary = "Eliminar plantilla de producto", description = "Elimina lógicamente una plantilla de producto (soft delete)")
    @APIResponses({
            @APIResponse(responseCode = "204", description = "Plantilla eliminada exitosamente"),
            @APIResponse(responseCode = "404", description = "Plantilla no encontrada"),
            @APIResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public Uni<Response> delete(@PathParam("id") Integer id) {
        log.info("REST: Deleting product template id: {}", id);

        return deleteUseCase.execute(id)
                .map(deleted -> {
                    if (deleted) {
                        return Response.noContent().build();
                    } else {
                        return Response.status(Response.Status.NOT_FOUND)
                                .entity(new ErrorResponse(404, "Not Found", "Plantilla de producto no encontrada con ID: " + id))
                                .build();
                    }
                })
                .onFailure()
                .recoverWithItem(this::mapExceptionToResponse);
    }

    @GET
    @WithTransaction
    @Operation(summary = "Listar plantillas de producto con paginación", description = "Obtiene un listado paginado de plantillas de producto con filtros opcionales")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Lista de plantillas obtenida exitosamente", content = @Content(
                    mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PagedResponse.class)
            )),
            @APIResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public Uni<Response> list(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("10") int size,
            @QueryParam("sortBy") @DefaultValue("name") String sortBy,
            @QueryParam("sortDirection") @DefaultValue("asc") String sortDirection,
            @QueryParam("search") String search,
            @QueryParam("name") String name,
            @QueryParam("internalReference") String internalReference,
            @QueryParam("type") String type,
            @QueryParam("categoryId") Integer categoryId,
            @QueryParam("brandId") Integer brandId,
            @QueryParam("status") String status,
            @QueryParam("canBeSold") String canBeSold,
            @QueryParam("canBePurchased") String canBePurchased,
            @QueryParam("hasVariants") String hasVariants,
            @QueryParam("includeDeleted") @DefaultValue("0") String includeDeleted
    ) {
        log.info("REST: Listing product templates with page: {}, size: {}", page, size);

        // Page is already 0-based (standard REST API convention)
        PageRequest pageRequest = PageRequest.builder()
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sortDirection(PageRequest.SortDirection.fromString(sortDirection))
                .build();

        ProductType productType = null;
        if (type != null && !type.isBlank()) {
            try {
                productType = ProductType.fromValue(type);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid product type: {}", type);
            }
        }

        ProductTemplateFilter filter = ProductTemplateFilter.builder()
                .search(search)
                .name(name)
                .internalReference(internalReference)
                .type(productType)
                .categoryId(categoryId)
                .brandId(brandId)
                .status(status)
                .canBeSold(canBeSold)
                .canBePurchased(canBePurchased)
                .hasVariants(hasVariants)
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
    @Operation(summary = "Listar todas las plantillas de producto sin paginación", description = "Obtiene todas las plantillas de producto sin paginación (optimizado para selects)")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Lista completa de plantillas obtenida exitosamente", content = @Content(
                    mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProductTemplateSelectResponse.class)
            )),
            @APIResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public Uni<Response> listAll(
            @QueryParam("search") String search,
            @QueryParam("name") String name,
            @QueryParam("internalReference") String internalReference,
            @QueryParam("type") String type,
            @QueryParam("categoryId") Integer categoryId,
            @QueryParam("brandId") Integer brandId,
            @QueryParam("status") @DefaultValue("active") String status,
            @QueryParam("canBeSold") String canBeSold,
            @QueryParam("canBePurchased") String canBePurchased,
            @QueryParam("hasVariants") String hasVariants,
            @QueryParam("includeDeleted") @DefaultValue("0") String includeDeleted
    ) {
        log.info("REST: Listing all product templates without pagination");

        ProductType productType = null;
        if (type != null && !type.isBlank()) {
            try {
                productType = ProductType.fromValue(type);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid product type: {}", type);
            }
        }

        ProductTemplateFilter filter = ProductTemplateFilter.builder()
                .search(search)
                .name(name)
                .internalReference(internalReference)
                .type(productType)
                .categoryId(categoryId)
                .brandId(brandId)
                .status(status)
                .canBeSold(canBeSold)
                .canBePurchased(canBePurchased)
                .hasVariants(hasVariants)
                .includeDeleted(includeDeleted)
                .build();

        return listAllUseCase.findAll(filter)
                .map(templates -> Response.ok(templates).build())
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

        if (t instanceof ProductTemplateNotFoundException) {
            status = Response.Status.NOT_FOUND;
        } else if (t instanceof DuplicateProductTemplateException) {
            status = Response.Status.CONFLICT;
        } else if (t instanceof InvalidProductTemplateException) {
            status = Response.Status.BAD_REQUEST;
        } else if (t instanceof ProductUomNotFoundException) {
            status = Response.Status.NOT_FOUND;
            message = "Unidad de medida no encontrada";
        } else if (t instanceof CurrencyNotFoundException) {
            status = Response.Status.NOT_FOUND;
            message = "Moneda no encontrada";
        } else if (t instanceof ProductAttributeNotFoundException) {
            status = Response.Status.NOT_FOUND;
            message = "Atributo de producto no encontrado: " + t.getMessage();
        } else if (t instanceof ProductAttributeValueNotFoundException) {
            status = Response.Status.NOT_FOUND;
            message = "Valor de atributo no encontrado: " + t.getMessage();
        } else if (t instanceof IllegalArgumentException) {
            status = Response.Status.BAD_REQUEST;
        } else {
            status = Response.Status.INTERNAL_SERVER_ERROR;
            message = "Ha ocurrido un error inesperado";
        }

        return Response.status(status)
                .entity(new ErrorResponse(status.getStatusCode(), status.getReasonPhrase(), message))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
