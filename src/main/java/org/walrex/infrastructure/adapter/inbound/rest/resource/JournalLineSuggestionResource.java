package org.walrex.infrastructure.adapter.inbound.rest.resource;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
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
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.walrex.application.dto.response.ErrorResponse;
import org.walrex.application.dto.response.JournalLineSuggestionResponse;
import org.walrex.application.port.input.AnalyzeAndSuggestJournalLineUseCase;

import java.util.List;

/**
 * REST endpoint para sugerencias de líneas de asiento contable basadas en análisis de documentos.
 *
 * Este endpoint combina:
 * - Azure Document Intelligence (OCR + extracción de datos)
 * - RAG Orchestrator (búsqueda vectorial + LLM)
 *
 * Para generar sugerencias inteligentes de cuentas contables y montos.
 */
@Slf4j
@Path("/api/v1/journal/suggestions")
@ApplicationScoped
@Tag(name = "Journal Entry Suggestions", description = "IA para sugerir líneas de asiento contable desde imágenes/PDFs")
public class JournalLineSuggestionResource {

    @Inject
    AnalyzeAndSuggestJournalLineUseCase analyzeAndSuggestUseCase;

    @POST
    @Path("/analyze-document")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Analizar documento y sugerir líneas de asiento",
            description = """
                    Analiza una imagen (factura, recibo, etc.) y genera sugerencias inteligentes
                    de líneas de asiento contable usando:
                    1. Azure Document Intelligence para extraer datos
                    2. RAG (búsqueda vectorial en Qdrant) para encontrar cuentas similares
                    3. LLM (Groq/Ollama) para generar sugerencias contextuales

                    Retorna una lista de líneas sugeridas con accountId, description, debit, credit.
                    """
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "Sugerencias generadas exitosamente",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = JournalLineSuggestionResponse.class)
                    )
            ),
            @APIResponse(
                    responseCode = "400",
                    description = "Documento inválido o formato no soportado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @APIResponse(
                    responseCode = "413",
                    description = "Documento demasiado grande (máximo 10 MB)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @APIResponse(
                    responseCode = "500",
                    description = "Error interno del servidor",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public Uni<Response> analyzeAndSuggest(
            @RestForm("file")
            @RequestBody(
                    description = "Imagen o PDF del documento a analizar (factura, recibo, etc.)",
                    required = true,
                    content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM)
            )
            FileUpload file,

            @RestForm("bookType")
            @DefaultValue("DIARIO")
            @RequestBody(
                    description = "Tipo de libro contable: DIARIO, COMPRAS, VENTAS, etc.",
                    required = false
            )
            String bookType
    ) {
        log.info("Received request to analyze document and suggest journal lines: {} (bookType: {})",
                file.fileName(), bookType);

        return analyzeAndSuggestUseCase.analyzeAndSuggest(file, bookType)
                .map(suggestions -> {
                    log.info("Successfully generated {} suggestions for: {}",
                            suggestions.size(), file.fileName());

                    return Response.ok(suggestions).build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    log.error("Error processing document: {}", file.fileName(), throwable);

                    int statusCode = determineStatusCode(throwable);
                    String errorType = getErrorType(statusCode);

                    ErrorResponse errorResponse = new ErrorResponse(
                            statusCode,
                            errorType,
                            "Error al procesar el documento: " + throwable.getMessage()
                    );

                    return Response.status(statusCode).entity(errorResponse).build();
                });
    }

    /**
     * Determina el código de estado HTTP basado en el tipo de excepción.
     */
    private int determineStatusCode(Throwable throwable) {
        String className = throwable.getClass().getSimpleName();

        return switch (className) {
            case "UnsupportedDocumentFormatException" -> 400;
            case "DocumentTooLargeException" -> 413;
            case "DocumentUnreadableException" -> 400;
            default -> 500;
        };
    }

    /**
     * Obtiene el tipo de error basado en el código de estado.
     */
    private String getErrorType(int statusCode) {
        return switch (statusCode) {
            case 400 -> "Bad Request";
            case 413 -> "Payload Too Large";
            case 500 -> "Internal Server Error";
            default -> "Error";
        };
    }
}
