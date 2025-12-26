package org.walrex.infrastructure.adapter.inbound.rest.router;

import io.quarkus.vertx.web.Route;
import io.quarkus.vertx.web.RouteBase;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.walrex.application.dto.response.TypeComprobantSunatSelectResponse;

@ApplicationScoped
@RouteBase(path = "/api/v1/typeComprobantsSunat", produces = "application/json")
@Tag(name = "Type Comprobants SUNAT", description = "API para tipos de comprobantes SUNAT")
public class TypeComprobantSunatRouter {

    @Inject
    TypeComprobantSunatHandler typeComprobantSunatHandler;

    /**
     * GET /api/v1/typeComprobantsSunat/all - Get all type comprobants SUNAT
     */
    @Route(path = "/all", methods = Route.HttpMethod.GET)
    @Operation(
        summary = "Listar todos los tipos de comprobantes SUNAT",
        description = "Obtiene todos los tipos de comprobantes SUNAT ordenados por código SUNAT. " +
                      "Optimizado para componentes de selección (select, dropdown, autocomplete)."
    )
    @APIResponses({
        @APIResponse(
            responseCode = "200",
            description = "Lista obtenida exitosamente",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = TypeComprobantSunatSelectResponse[].class),
                examples = @ExampleObject(
                    name = "Lista de tipos de comprobantes SUNAT",
                    value = """
                        [
                            {
                                "id": 1,
                                "sunatCode": "01",
                                "nameDocument": "Factura"
                            },
                            {
                                "id": 2,
                                "sunatCode": "03",
                                "nameDocument": "Boleta de Venta"
                            },
                            {
                                "id": 3,
                                "sunatCode": "07",
                                "nameDocument": "Nota de Crédito"
                            },
                            {
                                "id": 4,
                                "sunatCode": "08",
                                "nameDocument": "Nota de Débito"
                            }
                        ]
                        """
                )
            )
        ),
        @APIResponse(responseCode = "500", description = "Error interno del servidor", content = @Content(mediaType = "application/json"))
    })
    public Uni<Void> getAll(RoutingContext rc) {
        return typeComprobantSunatHandler.getAll(rc);
    }
}
