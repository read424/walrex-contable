package org.walrex.infrastructure.adapter.inbound.rest.router;

import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.walrex.application.dto.request.CreateCustomerRequest;
import org.walrex.application.dto.request.UpdateCustomerRequest;
import org.walrex.application.dto.response.CustomerResponse;

import io.quarkus.vertx.web.Route;
import io.quarkus.vertx.web.RouteBase;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.walrex.application.dto.response.PagedResponse;

@ApplicationScoped
@RouteBase(path = "api/v1/customers", produces = "application/json")
@Tag(name = "Customers", description = "Customers management")
public class CustomerRouter {

    @Inject
    CustomerHandler customerHandler;

    @Route(path = "", methods = Route.HttpMethod.POST)
    @Operation(summary = "Create a new customer", description = "Create a new customer in the system")
    @RequestBody(
        description = "Customer data to create",
        required = true,
        content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CreateCustomerRequest.class),
                examples = @ExampleObject(name = "Customer example", value = """
                    {
                        "idTypeDocument": 1,
                        "numberDocument": "12345678",
                        "lastName": "Doe",
                        "firstName": "John",
                        "address": "Calle 777 Urb. El cielo",
                        "idCountryDepartment": 7,
                        "idCountryProvince": 7,
                        "idCountryDistrict": 7,
                        "email": "john.doe@example.com",
                        "phoneNumber": "1234567890",
                        "phoneMobile": "123345555"
                    }
                """)
        )
    )
    @APIResponses({
        @APIResponse(responseCode = "201", description = "Customer created successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CustomerResponse.class))),
        @APIResponse(responseCode = "400", description = "Invalid request data", content = @Content(mediaType = "application/json")),
        @APIResponse(responseCode = "409", description = "Customer already exists", content = @Content(mediaType = "application/json"))
    })
    public Uni<Void> create(RoutingContext rc) {
        return customerHandler.create(rc);
    }

    @Route(path = "/:id", methods = Route.HttpMethod.PUT)
    @Operation(summary = "Update a customer", description = "Update an existing customer")
    @Parameter(name = "id", description = "Customer ID", required = true, in = ParameterIn.PATH, schema = @Schema(type = SchemaType.INTEGER))
    @RequestBody(description = "Customer data to update", required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = UpdateCustomerRequest.class), examples = @ExampleObject(name = "Customer update example", value = """
                {
                    "idTypeDocument": 1,
                    "numberDocument": "12345678",
                    "lastName": "Doe Updated",
                    "firstName": "John Updated",
                    "address": "Calle 777 Urb. El cielo Updated",
                    "idCountryDepartment": 7,
                    "idCountryProvince": 7,
                    "idCountryDistrict": 7,
                    "email": "john.doe.updated@example.com",
                    "phoneNumber": "1234567890"
                }
            """)))
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Customer updated successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CustomerResponse.class))),
            @APIResponse(responseCode = "400", description = "Invalid request data or ID format", content = @Content(mediaType = "application/json")),
            @APIResponse(responseCode = "404", description = "Customer not found", content = @Content(mediaType = "application/json")),
            @APIResponse(responseCode = "409", description = "Customer data conflict", content = @Content(mediaType = "application/json"))
    })
    public Uni<Void> update(RoutingContext rc) {
        return customerHandler.update(rc);
    }

    @Route(path = "", methods = Route.HttpMethod.GET)
    @Operation(summary = "List customers", description = "List customer pagination")
    @Parameter(name = "page", description = "Número de página (base 1, primera página = 1)", in = ParameterIn.QUERY, schema = @Schema(type = SchemaType.INTEGER, name = "page", defaultValue = "1"), example = "1")
    @Parameter(name = "size", description = "Tamaño de página (elementos por página)", in = ParameterIn.QUERY, schema = @Schema(type = SchemaType.INTEGER, defaultValue = "10", maximum = "100"), example = "10")
    @Parameter(name = "sortBy", description = "Campo por el cual ordenar", in = ParameterIn.QUERY, required = false, schema = @Schema(type = SchemaType.STRING, defaultValue = "id"), example = "name")
    @Parameter(name = "sortDirection", description = "Dirección del ordenamiento", in = ParameterIn.QUERY, required = false, schema = @Schema(type = SchemaType.STRING, defaultValue = "asc", enumeration = {
            "asc", "desc" }), example = "asc")
    @Parameter(name = "search", description = "Búsqueda general por nombre, apellido, razon social, numero de documento", in = ParameterIn.QUERY, required = false, schema = @Schema(type = SchemaType.STRING), example = "WALREX")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Lista de clientes obtenida exitosamente", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PagedResponse.class), examples = @ExampleObject(name = "Respuesta paginada", value = """
                        {
                            "content": [
                                {
                                    "idTypeDocument": 1,
                                    "numberDocument": "12345678",
                                    "lastName": "Doe",
                                    "firstName": "John",
                                    "address": "Calle 777 Urb. El cielo",
                                    "idCountryDepartment": 7,
                                    "idCountryProvince": 7,
                                    "idCountryDistrict": 7,
                                    "email": "john.doe@example.com",
                                    "phoneNumber": "1234567890"
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
                    """))),
            @APIResponse(responseCode = "400", description = "Parámetros de consulta inválidos", content = @Content(mediaType = "application/json"))
    })
    public Uni<Void> list(RoutingContext rc) {
        return customerHandler.list(rc);
    }

    @Route(path = "/:id", methods = Route.HttpMethod.GET)
    @Operation(summary = "Get customer by ID", description = "Get a customer by their unique ID")
    @Parameter(name = "id", description = "Customer ID", required = true, in = ParameterIn.PATH, schema = @Schema(type = SchemaType.INTEGER))
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Customer found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CustomerResponse.class))),
            @APIResponse(responseCode = "404", description = "Customer not found", content = @Content(mediaType = "application/json")),
            @APIResponse(responseCode = "400", description = "Invalid ID format", content = @Content(mediaType = "application/json"))
    })
    public Uni<Void> getById(RoutingContext rc) {
        return customerHandler.getById(rc);
    }
}
