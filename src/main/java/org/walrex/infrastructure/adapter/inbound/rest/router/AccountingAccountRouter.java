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
import org.walrex.application.dto.request.CreateAccountingAccountRequest;
import org.walrex.application.dto.request.UpdateAccountingAccountRequest;
import org.walrex.application.dto.response.AccountingAccountResponse;
import org.walrex.application.dto.response.AccountingAccountSelectResponse;

/**
 * Router de endpoints REST para cuentas contables.
 *
 * Define los endpoints HTTP y su documentación OpenAPI/Swagger.
 * Delega el manejo de peticiones a AccountingAccountHandler.
 */
@ApplicationScoped
@RouteBase(path = "/api/v1/accountingAccounts", produces = "application/json")
@Tag(name = "Accounting Accounts", description = "API para gestión de cuentas contables (plan de cuentas) (plan de cuentas)")
public class AccountingAccountRouter {

    @Inject
    AccountingAccountHandler accountHandler;

    /**
     * POST /api/v1/accountingAccounts - Create a new account
     */
    @Route(path = "", methods = Route.HttpMethod.POST)
    @Operation(
        summary = "Crear una nueva cuenta contable",
        description = "Crea una nueva cuenta en el plan de cuentas con validación de unicidad de código y nombre"
    )
    @RequestBody(
        description = "Datos de la cuenta a crear",
        required = true,
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = CreateAccountingAccountRequest.class),
            examples = @ExampleObject(
                name = "Cuenta Caja",
                value = """
                    {
                        "code": "1010",
                        "name": "Caja",
                        "type": "ASSET",
                        "normalSide": "DEBIT",
                        "active": true
                    }
                    """
            )
        )
    )
    @APIResponses({
        @APIResponse(
            responseCode = "201",
            description = "Cuenta creada exitosamente",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AccountingAccountResponse.class)
            )
        ),
        @APIResponse(
            responseCode = "400",
            description = "Datos inválidos",
            content = @Content(mediaType = "application/json")
        ),
        @APIResponse(
            responseCode = "409",
            description = "Conflicto - Ya existe una cuenta con ese código o nombre",
            content = @Content(mediaType = "application/json")
        )
    })
    public Uni<Void> create(RoutingContext rc) {
        return accountHandler.create(rc);
    }

    /**
     * GET /api/v1/accountingAccounts - List all accountingAccounts with pagination and filtering
     */
    @Route(path = "", methods = Route.HttpMethod.GET)
    @Operation(
        summary = "Listar cuentas con paginación",
        description = "Obtiene un listado paginado de cuentas contables con soporte para filtros y ordenamiento. " +
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
        schema = @Schema(type = SchemaType.INTEGER, defaultValue = "20", maximum = "100"),
        example = "10"
    )
    @Parameter(
        name = "sortBy",
        description = "Campo por el cual ordenar",
        in = ParameterIn.QUERY,
        required = false,
        schema = @Schema(type = SchemaType.STRING, defaultValue = "name"),
        example = "code"
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
        description = "Búsqueda general en código y nombre",
        in = ParameterIn.QUERY,
        required = false,
        schema = @Schema(type = SchemaType.STRING),
        example = "caja"
    )
    @Parameter(
        name = "type",
        description = "Filtrar por tipo de cuenta",
        in = ParameterIn.QUERY,
        required = false,
        schema = @Schema(type = SchemaType.STRING, enumeration = {"ASSET", "LIABILITY", "EQUITY", "REVENUE", "EXPENSE"}),
        example = "ASSET"
    )
    @Parameter(
        name = "normalSide",
        description = "Filtrar por lado normal",
        in = ParameterIn.QUERY,
        required = false,
        schema = @Schema(type = SchemaType.STRING, enumeration = {"DEBIT", "CREDIT"}),
        example = "DEBIT"
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
            description = "Lista de cuentas obtenida exitosamente",
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
                                    "code": "1010",
                                    "name": "Caja",
                                    "type": "ASSET",
                                    "normalSide": "DEBIT",
                                    "active": true,
                                    "createdAt": "2024-01-01T00:00:00Z",
                                    "updatedAt": "2024-01-01T00:00:00Z"
                                }
                            ],
                            "page": 1,
                            "size": 10,
                            "totalElements": 45,
                            "totalPages": 5,
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
        return accountHandler.list(rc);
    }

    /**
     * GET /api/v1/accountingAccounts/all - List all accountingAccounts without pagination
     */
    @Route(path = "/all", methods = Route.HttpMethod.GET)
    @Operation(
        summary = "Obtener todas las cuentas sin paginación",
        description = "Obtiene un listado completo de todas las cuentas activas sin paginación. " +
                      "Optimizado para componentes de selección (select, dropdown, autocomplete). " +
                      "Retorna solo los campos esenciales (id, código, nombre). " +
                      "Cacheado en Redis por 15 minutos."
    )
    @Parameter(
        name = "search",
        description = "Búsqueda general en código y nombre",
        in = ParameterIn.QUERY,
        required = false,
        schema = @Schema(type = SchemaType.STRING),
        example = "1010"
    )
    @Parameter(
        name = "type",
        description = "Filtrar por tipo de cuenta",
        in = ParameterIn.QUERY,
        required = false,
        schema = @Schema(type = SchemaType.STRING, enumeration = {"ASSET", "LIABILITY", "EQUITY", "REVENUE", "EXPENSE"}),
        example = "ASSET"
    )
    @Parameter(
        name = "active",
        description = "Filtrar por estado activo/inactivo (por defecto solo activas)",
        in = ParameterIn.QUERY,
        required = false,
        schema = @Schema(type = SchemaType.STRING),
        example = "1"
    )
    @APIResponses({
        @APIResponse(
            responseCode = "200",
            description = "Lista de cuentas obtenida exitosamente",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AccountingAccountSelectResponse.class, type = SchemaType.ARRAY),
                examples = @ExampleObject(
                    name = "Lista de cuentas para select",
                    value = """
                        [
                            {
                                "id": 1,
                                "code": "1010",
                                "name": "Caja"
                            },
                            {
                                "id": 2,
                                "code": "1020",
                                "name": "Bancos"
                            },
                            {
                                "id": 3,
                                "code": "2010",
                                "name": "Cuentas por Pagar"
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
        return accountHandler.findAll(rc);
    }

    /**
     * GET /api/v1/accountingAccounts/{id} - Get an accountingAccount by ID
     */
    @Route(path = "/:id", methods = Route.HttpMethod.GET)
    @Operation(
        summary = "Obtener cuenta por ID",
        description = "Recupera una cuenta específica por su identificador único"
    )
    @Parameter(
        name = "id",
        description = "ID único de la cuenta",
        required = true,
        in = ParameterIn.PATH,
        schema = @Schema(type = SchemaType.INTEGER),
        example = "1"
    )
    @APIResponses({
        @APIResponse(
            responseCode = "200",
            description = "Cuenta encontrada",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AccountingAccountResponse.class)
            )
        ),
        @APIResponse(
            responseCode = "404",
            description = "Cuenta no encontrada",
            content = @Content(mediaType = "application/json")
        )
    })
    public Uni<Void> getById(RoutingContext rc) {
        return accountHandler.getById(rc);
    }

    /**
     * PUT /api/v1/accountingAccounts/{id} - Update an account
     */
    @Route(path = "/:id", methods = Route.HttpMethod.PUT)
    @Operation(
        summary = "Actualizar una cuenta",
        description = "Actualiza los datos de una cuenta existente. Invalida automáticamente el cache."
    )
    @Parameter(
        name = "id",
        description = "ID único de la cuenta a actualizar",
        in = ParameterIn.PATH,
        required = true,
        schema = @Schema(type = SchemaType.INTEGER),
        example = "1"
    )
    @RequestBody(
        description = "Datos actualizados de la cuenta",
        required = true,
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = UpdateAccountingAccountRequest.class),
            examples = @ExampleObject(
                name = "Actualizar cuenta",
                value = """
                    {
                        "code": "1010",
                        "name": "Caja General",
                        "type": "ASSET",
                        "normalSide": "DEBIT",
                        "active": true
                    }
                    """
            )
        )
    )
    @APIResponses({
        @APIResponse(
            responseCode = "200",
            description = "Cuenta actualizada exitosamente",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AccountingAccountResponse.class)
            )
        ),
        @APIResponse(
            responseCode = "400",
            description = "Datos inválidos",
            content = @Content(mediaType = "application/json")
        ),
        @APIResponse(
            responseCode = "404",
            description = "Cuenta no encontrada",
            content = @Content(mediaType = "application/json")
        ),
        @APIResponse(
            responseCode = "409",
            description = "Conflicto - Ya existe otra cuenta con ese código o nombre",
            content = @Content(mediaType = "application/json")
        )
    })
    public Uni<Void> update(RoutingContext rc) {
        return accountHandler.update(rc);
    }

    /**
     * DELETE /api/v1/accountingAccounts/{id} - Delete an accountingAccount (soft delete)
     */
    @Route(path = "/:id", methods = Route.HttpMethod.DELETE)
    @Operation(
        summary = "Eliminar una cuenta (soft delete)",
        description = "Marca una cuenta como eliminada sin removerla físicamente de la base de datos. " +
                      "Invalida automáticamente el cache."
    )
    @Parameter(
        name = "id",
        description = "ID único de la cuenta a eliminar",
        in = ParameterIn.PATH,
        required = true,
        schema = @Schema(type = SchemaType.INTEGER),
        example = "1"
    )
    @APIResponses({
        @APIResponse(
            responseCode = "204",
            description = "Cuenta eliminada exitosamente (No Content)"
        ),
        @APIResponse(
            responseCode = "404",
            description = "Cuenta no encontrada",
            content = @Content(mediaType = "application/json")
        )
    })
    public Uni<Void> delete(RoutingContext rc) {
        return accountHandler.delete(rc);
    }

    /**
     * PUT /api/v1/accountingAccounts/{id}/restore - Restore a deleted account
     */
    @Route(path = "/:id/restore", methods = Route.HttpMethod.PUT)
    @Operation(
        summary = "Restaurar una cuenta eliminada",
        description = "Restaura una cuenta previamente eliminada (soft delete). " +
                      "Invalida automáticamente el cache."
    )
    @Parameter(
        name = "id",
        description = "ID único de la cuenta a restaurar",
        in = ParameterIn.PATH,
        required = true,
        schema = @Schema(type = SchemaType.INTEGER),
        example = "1"
    )
    @APIResponses({
        @APIResponse(
            responseCode = "200",
            description = "Cuenta restaurada exitosamente",
            content = @Content(mediaType = "application/json")
        ),
        @APIResponse(
            responseCode = "404",
            description = "Cuenta no encontrada o no estaba eliminada",
            content = @Content(mediaType = "application/json")
        )
    })
    public Uni<Void> restore(RoutingContext rc) {
        return accountHandler.restore(rc);
    }
}
