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
import org.walrex.application.dto.request.CreateCountryRequest;
import org.walrex.application.dto.request.UpdateCountryRequest;
import org.walrex.application.dto.response.AvailabilityResponse;
import org.walrex.application.dto.response.CountryResponse;
import org.walrex.application.dto.response.PagedResponse;

@ApplicationScoped
@RouteBase(path = "/api/v1/countries", produces = "application/json")
@Tag(name = "Countries", description = "API para gestión de paises ISO 3166")
public class CountryRouter {

    @Inject
    CountryHandler countryHandler;

    @Inject
    CountryCurrencyHandler countryCurrencyHandler;

    @Route(path = "", methods = Route.HttpMethod.POST)
    @Operation(
            summary = "Crear un nuevo pais",
            description = "Crea un nuevo pais en el sistema con código ISO 3166"
    )
    @RequestBody(
            description = "Datos del país a crear",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = CreateCountryRequest.class),
                    examples = @ExampleObject(
                            name = "Venezuela",
                            value = """
                                    {
                                        "alphabeticCode": "VEN",
                                        "numericCode": 862,
                                        "name": "VENEZUELA"
                                    }
                                    """
                    )
            )
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "201",
                    description = "Pais creado exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CountryResponse.class)
                    )
            ),
            @APIResponse(
                    responseCode = "400",
                    description = "Datos inválidos o país duplicado",
                    content = @Content(mediaType = "application/json")
            ),
            @APIResponse(
                    responseCode = "409",
                    description = "Conflicto - Ya existe un pais con ese código",
                    content = @Content(mediaType = "application/json")
            )
    })
    public Uni<Void> create(RoutingContext rc){
        return countryHandler.create(rc);
    }

    /**
     * GET /api/v1/countries - List all countries with pagination and filtering
     */
    @Route(path = "", methods = Route.HttpMethod.GET)
    @Operation(
            summary = "Listar paises con paginación",
            description = "Obtiene un listado paginado de paises con soporte para filtros y ordenamiento. " +
                    "Los resultados se cachean en Redis por 5 minutos para mejorar el rendimiento."
    )
    @Parameter(
            name = "page",
            description = "Número de página (base 1, primera página = 1)",
            in = ParameterIn.QUERY,
            schema = @Schema(type = SchemaType.INTEGER, defaultValue = "1"),
            example = "1"
    )
    @Parameter(
            name = "size",
            description = "Tamaño de página (elementos por página)",
            in = ParameterIn.QUERY,
            schema = @Schema(type = SchemaType.INTEGER, defaultValue = "10", maximum = "100"),
            example = "10"
    )
    @Parameter(
            name = "sortBy",
            description = "Campo por el cual ordenar",
            in = ParameterIn.QUERY,
            schema = @Schema(type = SchemaType.STRING, defaultValue = "id"),
            example = "name"
    )
    @Parameter(
            name = "includeDeleted",
            description = "Campo para agregar registros deshabilitados",
            in = ParameterIn.QUERY,
            schema = @Schema(type = SchemaType.STRING, defaultValue = "0", enumeration = {"1", "0"}),
            required = false,
            example = "1"
    )
    @Parameter(
            name = "sortDirection",
            description = "Dirección del ordenamiento",
            in = ParameterIn.QUERY,
            schema = @Schema(type = SchemaType.STRING, defaultValue = "asc", enumeration = {"asc", "desc"}),
            required = false,
            example = "asc"
    )
    @Parameter(
            name = "search",
            description = "Búsqueda general en nombre y código alfabético",
            in = ParameterIn.QUERY,
            schema = @Schema(type = SchemaType.STRING),
            required = false,
            example = "VEN"
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "Lista de paises obtenida exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PagedResponse.class),
                            examples = @ExampleObject(
                                    name = "Respuesta paginada",
                                    value = """
                        {
                            "content": [
                                {
                                    "id": 1,
                                    "alphabeticCode": "PER",
                                    "numericCode": "604",
                                    "name": "PERU",
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
        return countryHandler.list(rc);
    }

    /**
     * GET /api/v1/countries/{id} - Get a country by ID
     */
    @Route(path = "/:id", methods = Route.HttpMethod.GET)
    @Operation(
            summary = "Obtener pais por ID",
            description = "Recupera un país específico por su identificador único"
    )
    @Parameter(
            name = "id",
            description = "ID único del país",
            required = true,
            in = ParameterIn.PATH,
            schema = @Schema(type = SchemaType.INTEGER),
            example = "1"
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "País encontrado",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CountryResponse.class)
                    )
            ),
            @APIResponse(
                    responseCode = "404",
                    description = "País no encontrado",
                    content = @Content(mediaType = "application/json")
            )
    })
    public Uni<Void> getById(RoutingContext rc) {
        return countryHandler.getById(rc);
    }

    /**
     * PUT /api/v1/countries/{id} - Update a country
     */
    @Route(path = "/:id", methods = Route.HttpMethod.PUT)
    @Operation(
            summary = "Actualizar un país",
            description = "Actualiza los datos de un país existente. Invalida automáticamente el cache."
    )
    @Parameter(
            name = "id",
            description = "ID único del país a actualizar",
            in = ParameterIn.PATH,
            required = true,
            schema = @Schema(type = SchemaType.INTEGER),
            example = "1"
    )
    @RequestBody(
            description = "Datos actualizados del país",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = UpdateCountryRequest.class),
                    examples = @ExampleObject(
                            name = "Actualizar país",
                            value = """
                    {
                        "alphabeticCode": "PER",
                        "numericCode": 604,
                        "phoneCode": 51,
                        "name": "PERU"
                    }
                    """
                    )
            )
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "País actualizado exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CountryResponse.class)
                    )
            ),
            @APIResponse(
                    responseCode = "400",
                    description = "Datos inválidos",
                    content = @Content(mediaType = "application/json")
            ),
            @APIResponse(
                    responseCode = "404",
                    description = "País no encontrada",
                    content = @Content(mediaType = "application/json")
            ),
            @APIResponse(
                    responseCode = "409",
                    description = "Conflicto - Ya existe otro país con ese código",
                    content = @Content(mediaType = "application/json")
            )
    })
    public Uni<Void> update(RoutingContext rc) {
        return countryHandler.update(rc);
    }

    /**
     * DELETE /api/v1/countries/{id} - Delete a country (soft delete)
     */
    @Route(path = "/:id", methods = Route.HttpMethod.DELETE)
    @Operation(
            summary = "Eliminar un país (soft delete)",
            description = "Marca un país como eliminado sin removerla físicamente de la base de datos. " +
                    "Invalida automáticamente el cache."
    )
    @Parameter(
            name = "id",
            description = "ID único del país a eliminar",
            in = ParameterIn.PATH,
            required = true,
            schema = @Schema(type = SchemaType.INTEGER),
            example = "1"
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "204",
                    description = "País eliminada exitosamente (No Content)"
            ),
            @APIResponse(
                    responseCode = "404",
                    description = "País no encontrada",
                    content = @Content(mediaType = "application/json")
            )
    })
    public Uni<Void> delete(RoutingContext rc) {
        return countryHandler.delete(rc);
    }

    /**
     * GET /api/v1/countries/check-availability - Check if a field value is
     * available
     * Query params: alphabeticCode, numericCode, or name
     */
    @Route(path = "/check-availability", methods = Route.HttpMethod.GET)
    @Operation(
            summary = "Verificar disponibilidad de un campo",
            description = "Verifica si un valor específico (código alfabético, código numérico o nombre) " +
                    "está disponible o ya existe en otro país. Útil para validaciones en tiempo real."
    )
    @Parameter(
            name = "alphabeticCode",
            description = "Código alfabético ISO 3166 a verificar (ej: PER)",
            in = ParameterIn.QUERY,
            schema = @Schema(type = SchemaType.STRING),
            example = "PER"
    )
    @Parameter(
            name = "numericCode",
            description = "Código numérico ISO 3166 a verificar (ej: 604)",
            in = ParameterIn.QUERY,
            schema = @Schema(type = SchemaType.INTEGER),
            example = "604"
    )
    @Parameter(
            name = "name",
            description = "Nombre del país a verificar",
            in = ParameterIn.QUERY,
            schema = @Schema(type = SchemaType.STRING),
            example = "Perú"
    )
    @Parameter(
            name = "excludeId",
            description = "ID de país a excluir de la verificación (útil para actualizaciones)",
            in = ParameterIn.QUERY,
            schema = @Schema(type = SchemaType.INTEGER),
            example = "1"
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "Verificación completada",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AvailabilityResponse.class),
                            examples = @ExampleObject(
                                    name = "Campo disponible",
                                    value = """
                        {
                            "field": "alphabeticCode",
                            "value": "PER",
                            "available": true
                        }
                        """
                            )
                    )
            ),
            @APIResponse(
                    responseCode = "400",
                    description = "Parámetros inválidos - Se requiere al menos un campo a verificar",
                    content = @Content(mediaType = "application/json")
            )
    })
    public Uni<Void> checkAvailability(RoutingContext rc) {
        return countryHandler.checkAvailability(rc);
    }

    // ==================== Country-Currency Endpoints ====================

    /**
     * GET /api/v1/countries/{countryId}/currencies - List currencies of a country
     */
    @Route(path = "/:countryId/currencies", methods = Route.HttpMethod.GET)
    @Operation(
            summary = "Listar monedas de un país",
            description = "Obtiene todas las monedas asignadas a un país específico"
    )
    @Parameter(
            name = "countryId",
            description = "ID del país",
            required = true,
            in = ParameterIn.PATH,
            schema = @Schema(type = SchemaType.INTEGER),
            example = "1"
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "Lista de monedas obtenida exitosamente",
                    content = @Content(mediaType = "application/json")
            ),
            @APIResponse(
                    responseCode = "404",
                    description = "País no encontrado",
                    content = @Content(mediaType = "application/json")
            )
    })
    public Uni<Void> listCurrencies(RoutingContext rc) {
        return countryCurrencyHandler.listCurrencies(rc);
    }

    /**
     * POST /api/v1/countries/{countryId}/currencies/{currencyId} - Assign currency to country
     */
    @Route(path = "/:countryId/currencies/:currencyId", methods = Route.HttpMethod.POST)
    @Operation(
            summary = "Asignar moneda a un país",
            description = "Crea una relación entre un país y una moneda"
    )
    @Parameter(
            name = "countryId",
            description = "ID del país",
            required = true,
            in = ParameterIn.PATH,
            schema = @Schema(type = SchemaType.INTEGER),
            example = "1"
    )
    @Parameter(
            name = "currencyId",
            description = "ID de la moneda",
            required = true,
            in = ParameterIn.PATH,
            schema = @Schema(type = SchemaType.INTEGER),
            example = "1"
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "201",
                    description = "Moneda asignada exitosamente",
                    content = @Content(mediaType = "application/json")
            ),
            @APIResponse(
                    responseCode = "400",
                    description = "Datos inválidos",
                    content = @Content(mediaType = "application/json")
            ),
            @APIResponse(
                    responseCode = "409",
                    description = "La moneda ya está asignada al país",
                    content = @Content(mediaType = "application/json")
            )
    })
    public Uni<Void> assignCurrency(RoutingContext rc) {
        return countryCurrencyHandler.assignCurrency(rc);
    }

    /**
     * PUT /api/v1/countries/{countryId}/currencies/{currencyId}/default - Set currency as default
     */
    @Route(path = "/:countryId/currencies/:currencyId/default", methods = Route.HttpMethod.PUT)
    @Operation(
            summary = "Establecer moneda predeterminada",
            description = "Define una moneda como la predeterminada para un país"
    )
    @Parameter(
            name = "countryId",
            description = "ID del país",
            required = true,
            in = ParameterIn.PATH,
            schema = @Schema(type = SchemaType.INTEGER),
            example = "1"
    )
    @Parameter(
            name = "currencyId",
            description = "ID de la moneda",
            required = true,
            in = ParameterIn.PATH,
            schema = @Schema(type = SchemaType.INTEGER),
            example = "1"
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "Moneda establecida como predeterminada",
                    content = @Content(mediaType = "application/json")
            ),
            @APIResponse(
                    responseCode = "400",
                    description = "La moneda no está asignada al país",
                    content = @Content(mediaType = "application/json")
            ),
            @APIResponse(
                    responseCode = "404",
                    description = "País o moneda no encontrado",
                    content = @Content(mediaType = "application/json")
            )
    })
    public Uni<Void> setDefaultCurrency(RoutingContext rc) {
        return countryCurrencyHandler.setDefaultCurrency(rc);
    }

    /**
     * PUT /api/v1/countries/{countryId}/currencies/{currencyId}/operation - Update operational status
     */
    @Route(path = "/:countryId/currencies/:currencyId/operation", methods = Route.HttpMethod.PUT)
    @Operation(
            summary = "Actualizar estado operacional de moneda",
            description = "Activa o desactiva la operación con una moneda en un país"
    )
    @Parameter(
            name = "countryId",
            description = "ID del país",
            required = true,
            in = ParameterIn.PATH,
            schema = @Schema(type = SchemaType.INTEGER),
            example = "1"
    )
    @Parameter(
            name = "currencyId",
            description = "ID de la moneda",
            required = true,
            in = ParameterIn.PATH,
            schema = @Schema(type = SchemaType.INTEGER),
            example = "1"
    )
    @RequestBody(
            description = "Estado operacional",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            name = "Activar moneda",
                            value = """
                            {
                                "isOperational": true
                            }
                            """
                    )
            )
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "Estado operacional actualizado",
                    content = @Content(mediaType = "application/json")
            ),
            @APIResponse(
                    responseCode = "400",
                    description = "Datos inválidos",
                    content = @Content(mediaType = "application/json")
            ),
            @APIResponse(
                    responseCode = "404",
                    description = "Relación país-moneda no encontrada",
                    content = @Content(mediaType = "application/json")
            )
    })
    public Uni<Void> updateOperationalStatus(RoutingContext rc) {
        return countryCurrencyHandler.updateOperationalStatus(rc);
    }

    /**
     * GET /api/v1/countries/{countryId}/default-currency - Get default currency
     */
    @Route(path = "/:countryId/default-currency", methods = Route.HttpMethod.GET)
    @Operation(
            summary = "Obtener moneda predeterminada",
            description = "Recupera la moneda predeterminada de un país"
    )
    @Parameter(
            name = "countryId",
            description = "ID del país",
            required = true,
            in = ParameterIn.PATH,
            schema = @Schema(type = SchemaType.INTEGER),
            example = "1"
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "Moneda predeterminada obtenida",
                    content = @Content(mediaType = "application/json")
            ),
            @APIResponse(
                    responseCode = "404",
                    description = "País no tiene moneda predeterminada",
                    content = @Content(mediaType = "application/json")
            )
    })
    public Uni<Void> getDefaultCurrency(RoutingContext rc) {
        return countryCurrencyHandler.getDefaultCurrency(rc);
    }

}
