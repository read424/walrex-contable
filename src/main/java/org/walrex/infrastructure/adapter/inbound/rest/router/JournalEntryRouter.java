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
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.walrex.application.dto.request.CreateJournalEntryRequest;
import org.walrex.application.dto.response.JournalEntryResponse;

/**
 * REST Router for Journal Entry (Asiento Contable) operations.
 * Defines HTTP endpoints and delegates handling to JournalEntryHandler.
 */
@ApplicationScoped
@RouteBase(path = "/api/v1/journal-entries", produces = "application/json")
@Tag(name = "Journal Entries", description = "API para gestión de asientos contables")
public class JournalEntryRouter {

    @Inject
    JournalEntryHandler journalEntryHandler;

    /**
     * POST /api/v1/journal-entries - Create a new journal entry
     */
    @Route(path = "", methods = Route.HttpMethod.POST)
    @Operation(
            summary = "Crear un nuevo asiento contable",
            description = "Crea un nuevo asiento contable con sus líneas de detalle. " +
                    "El asiento debe estar balanceado (suma de débitos = suma de créditos). " +
                    "El sistema genera automáticamente los correlativos (operation_number y book_correlative)."
    )
    @RequestBody(
            description = "Datos del asiento contable a crear",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = CreateJournalEntryRequest.class),
                    examples = @ExampleObject(
                            name = "Asiento de apertura",
                            value = """
                                    {
                                        "entryDate": "2024-01-01",
                                        "bookType": "DIARIO",
                                        "description": "Asiento de apertura del ejercicio 2024",
                                        "reference": null,
                                        "lines": [
                                            {
                                                "accountId": 1,
                                                "debit": 10000.00,
                                                "credit": 0.00,
                                                "description": "Caja - Aporte inicial"
                                            },
                                            {
                                                "accountId": 2,
                                                "debit": 0.00,
                                                "credit": 10000.00,
                                                "description": "Capital Social"
                                            }
                                        ]
                                    }
                                    """
                    )
            )
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "201",
                    description = "Asiento contable creado exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = JournalEntryResponse.class),
                            examples = @ExampleObject(
                                    name = "Asiento creado",
                                    value = """
                                            {
                                                "id": 1,
                                                "bookType": "DIARIO",
                                                "entryDate": "2024-01-01",
                                                "operationNumber": 1,
                                                "bookCorrelative": 1,
                                                "description": "Asiento de apertura del ejercicio 2024",
                                                "status": "ACTIVE",
                                                "lines": [
                                                    {
                                                        "id": 1,
                                                        "journalEntryId": 1,
                                                        "accountId": 1,
                                                        "debit": 10000.00,
                                                        "credit": 0.00,
                                                        "description": "Caja - Aporte inicial"
                                                    },
                                                    {
                                                        "id": 2,
                                                        "journalEntryId": 1,
                                                        "accountId": 2,
                                                        "debit": 0.00,
                                                        "credit": 10000.00,
                                                        "description": "Capital Social"
                                                    }
                                                ],
                                                "totalDebit": 10000.00,
                                                "totalCredit": 10000.00,
                                                "createdAt": "2024-01-01T10:00:00Z",
                                                "updatedAt": "2024-01-01T10:00:00Z"
                                            }
                                            """
                            )
                    )
            ),
            @APIResponse(
                    responseCode = "400",
                    description = "Datos inválidos - Asiento desbalanceado o validación fallida",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "Asiento desbalanceado",
                                            value = """
                                                    {
                                                        "code": "UNBALANCED_ENTRY",
                                                        "message": "Journal entry is not balanced. Debits: 10000.00, Credits: 5000.00, Difference: 5000.00",
                                                        "details": {
                                                            "totalDebit": "10000.00",
                                                            "totalCredit": "5000.00",
                                                            "difference": "5000.00"
                                                        }
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Líneas insuficientes",
                                            value = """
                                                    {
                                                        "code": "INVALID_ENTRY",
                                                        "message": "A journal entry must have at least 2 lines",
                                                        "details": "lines"
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @APIResponse(
                    responseCode = "500",
                    description = "Error interno del servidor",
                    content = @Content(mediaType = "application/json")
            )
    })
    public Uni<Void> create(RoutingContext rc) {
        return journalEntryHandler.create(rc);
    }

    /**
     * GET /api/v1/journal-entries - List journal entries with pagination
     */
    @Route(path = "", methods = Route.HttpMethod.GET)
    @Operation(
            summary = "Listar asientos contables",
            description = "Obtiene una lista paginada de asientos contables con filtros opcionales. " +
                    "Soporta filtros por año, mes, tipo de libro y búsqueda en descripción."
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "Lista de asientos contables obtenida exitosamente",
                    content = @Content(mediaType = "application/json")
            ),
            @APIResponse(
                    responseCode = "400",
                    description = "Parámetros inválidos",
                    content = @Content(mediaType = "application/json")
            ),
            @APIResponse(
                    responseCode = "500",
                    description = "Error interno del servidor",
                    content = @Content(mediaType = "application/json")
            )
    })
    public Uni<Void> list(RoutingContext rc) {
        return journalEntryHandler.list(rc);
    }
}
