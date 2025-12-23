package org.walrex.infrastructure.adapter.inbound.rest.router;

import io.quarkus.vertx.web.Route;
import io.quarkus.vertx.web.RouteBase;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.walrex.application.dto.request.CreateCurrencyRequest;
import org.walrex.application.dto.request.UpdateCurrencyRequest;
import org.walrex.application.dto.response.CurrencyResponse;
import org.walrex.application.dto.response.CurrencySelectResponse;

@ApplicationScoped
@RouteBase(path = "/api/v1/currencies", produces = "application/json")
@Tag(name = "Currencies", description = "API para gestión de monedas ISO 4217")
public class CurrencyRouter {

    @Inject
    CurrencyHandler currencyHandler;

    /**
     * POST /api/v1/currencies - Create a new currency
     */
    @Route(path = "", methods = Route.HttpMethod.POST)
    @Operation(
        summary = "Crear una nueva moneda",
        description = "Crea una nueva moneda en el sistema con código ISO 4217"
    )
    @RequestBody(
        description = "Datos de la moneda a crear",
        required = true,
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = CreateCurrencyRequest.class),
            examples = @ExampleObject(
                name = "Dólar Estadounidense",
                value = """
                    {
                        "alphabeticCode": "USD",
                        "numericCode": 840,
                        "name": "Dólar Estadounidense"
                    }
                    """
            )
        )
    )
    @APIResponses({
        @APIResponse(
            responseCode = "201",
            description = "Moneda creada exitosamente",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CurrencyResponse.class)
            )
        ),
        @APIResponse(
            responseCode = "400",
            description = "Datos inválidos o moneda duplicada",
            content = @Content(mediaType = "application/json")
        ),
        @APIResponse(
            responseCode = "409",
            description = "Conflicto - Ya existe una moneda con ese código",
            content = @Content(mediaType = "application/json")
        )
    })
    public Uni<Void> create(RoutingContext rc) {
        return currencyHandler.create(rc);
    }

    /**
     * GET /api/v1/currencies - List all currencies with pagination and filtering
     */
    @Route(path = "", methods = Route.HttpMethod.GET)
    @Operation(
        summary = "Listar monedas con paginación",
        description = "Obtiene un listado paginado de monedas con soporte para filtros y ordenamiento. " +
                      "Los resultados se cachean en Redis por 5 minutos para mejorar el rendimiento."
    )
    @Parameter(
        name = "page",
        description = "Número de página (base 1, primera página = 1)",
        in = ParameterIn.QUERY,
        schema = @Schema(type = SchemaType.INTEGER, name = "page", defaultValue = "1"),
        example = "1"
    )
    @Parameter(
        name = "size",
        description = "Tamaño de página (elementos por página)",
        in = ParameterIn.QUERY,
        schema = @Schema(type = SchemaType.INTEGER, defaultValue = "20", maximum = "100"),
        example = "10"
    )
    @Parameter(
        name = "sortBy",
        description = "Campo por el cual ordenar",
        in = ParameterIn.QUERY,
        required = false,
        schema = @Schema(type = SchemaType.STRING, defaultValue = "id"),
        example = "name"
    )
    @Parameter(
        name = "sortDirection",
        description = "Dirección del ordenamiento",
        in = ParameterIn.QUERY,
        required = false,
        schema = @Schema(type = SchemaType.STRING, defaultValue = "asc", enumeration = {"asc", "desc"}),
        example = "asc"
    )
    @Parameter(
        name = "search",
        description = "Búsqueda general en nombre y código alfabético",
        in = ParameterIn.QUERY,
        required = false,
        schema = @Schema(type = SchemaType.STRING),
        example = "USD"
    )
    @Parameter(
        name = "active",
        description = "Filtrar por estado activo/inactivo",
        in = ParameterIn.QUERY,
        required = false,
        schema = @Schema(type = SchemaType.STRING),
        example = "1"
    )
    @APIResponses({
        @APIResponse(
            responseCode = "200",
            description = "Lista de monedas obtenida exitosamente",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = org.walrex.application.dto.response.PagedResponse.class),
                examples = @ExampleObject(
                    name = "Respuesta paginada",
                    value = """
                        {
                            "content": [
                                {
                                    "id": 1,
                                    "alphabeticCode": "USD",
                                    "numericCode": "840",
                                    "name": "Dólar Estadounidense",
                                    "active": true,
                                    "createdAt": "2024-01-01T00:00:00Z",
                                    "updatedAt": "2024-01-01T00:00:00Z"
                                }
                            ],
                            "page": 1,
                            "size": 10,
                            "totalElements": 150,
                            "totalPages": 15,
                            "first": true,
                            "last": false,
                            "empty": false
                        }
                        """
                )
            )
        ),
        @APIResponse(
            responseCode = "400",
            description = "Parámetros de consulta inválidos",
            content = @Content(mediaType = "application/json")
        )
    })
    public Uni<Void> list(RoutingContext rc) {
        return currencyHandler.list(rc);
    }

    /**
     * GET /api/v1/currencies/all - List all currencies without pagination
     */
    @Route(path = "/all", methods = Route.HttpMethod.GET)
    @Operation(
        summary = "Obtener todas las monedas sin paginación",
        description = "Obtiene un listado completo de todas las monedas activas sin paginación. " +
                      "Optimizado para componentes de selección (select, dropdown, autocomplete). " +
                      "Retorna solo los campos esenciales (id, código alfabético, código numérico, nombre)."
    )
    @Parameter(
        name = "search",
        description = "Búsqueda general en nombre y código alfabético",
        in = ParameterIn.QUERY,
        required = false,
        schema = @Schema(type = SchemaType.STRING),
        example = "USD"
    )
    @Parameter(
        name = "active",
        description = "Filtrar por estado activo/inactivo (por defecto solo activas)",
        in = ParameterIn.QUERY,
        required = false,
        schema = @Schema(type = SchemaType.STRING),
        example = "1"
    )
    @Parameter(
        name = "includeInactive",
        description = "Incluir monedas inactivas en el resultado",
        in = ParameterIn.QUERY,
        required = false,
        schema = @Schema(type = SchemaType.BOOLEAN, defaultValue = "false"),
        example = "false"
    )
    @APIResponses({
        @APIResponse(
            responseCode = "200",
            description = "Lista de monedas obtenida exitosamente",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CurrencySelectResponse.class, type = SchemaType.ARRAY),
                examples = @ExampleObject(
                    name = "Lista de monedas para select",
                    value = """
                        [
                            {
                                "id": 1,
                                "alphabeticCode": "USD",
                                "numericCode": "840",
                                "name": "Dólar Estadounidense"
                            },
                            {
                                "id": 2,
                                "alphabeticCode": "EUR",
                                "numericCode": "978",
                                "name": "Euro"
                            },
                            {
                                "id": 3,
                                "alphabeticCode": "PEN",
                                "numericCode": "604",
                                "name": "Sol Peruano"
                            }
                        ]
                        """
                )
            )
        ),
        @APIResponse(
            responseCode = "400",
            description = "Parámetros de consulta inválidos",
            content = @Content(mediaType = "application/json")
        )
    })
    public Uni<Void> findAll(RoutingContext rc) {
        return currencyHandler.findAll(rc);
    }

    /**
     * GET /api/v1/currencies/{id} - Get a currency by ID
     */
    @Route(path = "/:id", methods = Route.HttpMethod.GET)
    @Operation(
        summary = "Obtener moneda por ID",
        description = "Recupera una moneda específica por su identificador único"
    )
    @Parameter(
        name = "id",
        description = "ID único de la moneda",
        required = true,
        in = ParameterIn.PATH,
        schema = @Schema(type = SchemaType.INTEGER),
        example = "1"
    )
    @APIResponses({
        @APIResponse(
            responseCode = "200",
            description = "Moneda encontrada",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CurrencyResponse.class)
            )
        ),
        @APIResponse(
            responseCode = "404",
            description = "Moneda no encontrada",
            content = @Content(mediaType = "application/json")
        )
    })
    public Uni<Void> getById(RoutingContext rc) {
        return currencyHandler.getById(rc);
    }

    /**
     * PUT /api/v1/currencies/{id} - Update a currency
     */
    @Route(path = "/:id", methods = Route.HttpMethod.PUT)
    @Operation(
        summary = "Actualizar una moneda",
        description = "Actualiza los datos de una moneda existente. Invalida automáticamente el cache."
    )
    @Parameter(
        name = "id",
        description = "ID único de la moneda a actualizar",
        in = ParameterIn.PATH,
        required = true,
        schema = @Schema(type = SchemaType.INTEGER),
        example = "1"
    )
    @RequestBody(
        description = "Datos actualizados de la moneda",
        required = true,
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = UpdateCurrencyRequest.class),
            examples = @ExampleObject(
                name = "Actualizar moneda",
                value = """
                    {
                        "alphabeticCode": "USD",
                        "numericCode": 840,
                        "name": "Dólar de los Estados Unidos"
                    }
                    """
            )
        )
    )
    @APIResponses({
        @APIResponse(
            responseCode = "200",
            description = "Moneda actualizada exitosamente",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CurrencyResponse.class)
            )
        ),
        @APIResponse(
            responseCode = "400",
            description = "Datos inválidos",
            content = @Content(mediaType = "application/json")
        ),
        @APIResponse(
            responseCode = "404",
            description = "Moneda no encontrada",
            content = @Content(mediaType = "application/json")
        ),
        @APIResponse(
            responseCode = "409",
            description = "Conflicto - Ya existe otra moneda con ese código",
            content = @Content(mediaType = "application/json")
        )
    })
    public Uni<Void> update(RoutingContext rc) {
        return currencyHandler.update(rc);
    }

    /**
     * DELETE /api/v1/currencies/{id} - Delete a currency (soft delete)
     */
    @Route(path = "/:id", methods = Route.HttpMethod.DELETE)
    @Operation(
        summary = "Eliminar una moneda (soft delete)",
        description = "Marca una moneda como eliminada sin removerla físicamente de la base de datos. " +
                      "Invalida automáticamente el cache."
    )
    @Parameter(
        name = "id",
        description = "ID único de la moneda a eliminar",
        in = ParameterIn.PATH,
        required = true,
        schema = @Schema(type = SchemaType.INTEGER),
        example = "1"
    )
    @APIResponses({
        @APIResponse(
            responseCode = "204",
            description = "Moneda eliminada exitosamente (No Content)"
        ),
        @APIResponse(
            responseCode = "404",
            description = "Moneda no encontrada",
            content = @Content(mediaType = "application/json")
        )
    })
    public Uni<Void> delete(RoutingContext rc) {
        return currencyHandler.delete(rc);
    }

}
