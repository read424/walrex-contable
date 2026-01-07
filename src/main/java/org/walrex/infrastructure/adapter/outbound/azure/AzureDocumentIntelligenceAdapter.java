package org.walrex.infrastructure.adapter.outbound.azure;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.walrex.application.port.output.DocumentIntelligencePort;
import org.walrex.domain.exception.DocumentIntelligenceApiException;
import org.walrex.domain.exception.DocumentUnreadableException;
import org.walrex.domain.model.DocumentAnalysisResult;
import org.walrex.infrastructure.adapter.logging.LogExecutionTime;
import org.walrex.infrastructure.adapter.outbound.azure.dto.AzureAnalyzeResponse;
import org.walrex.infrastructure.adapter.outbound.azure.mapper.AzureDocIntelMapper;

import java.time.Duration;

/**
 * Adapter que implementa el puerto de salida para comunicarse con Azure Document Intelligence.
 * Usa el patrón de arquitectura hexagonal donde este adapter pertenece a la capa de infraestructura.
 */
@Slf4j
@ApplicationScoped
public class AzureDocumentIntelligenceAdapter implements DocumentIntelligencePort {

    private static final String MODEL_ID = "prebuilt-invoice";
    private static final String API_VERSION = "2023-07-31";
    private static final int MAX_POLLING_ATTEMPTS = 30;
    private static final Duration POLLING_INTERVAL = Duration.ofSeconds(2);

    @Inject
    @RestClient
    AzureDocIntelRestClient azureRestClient;

    @Inject
    AzureDocIntelMapper mapper;

    @ConfigProperty(name = "azure.doc.intel.key")
    String azureApiKey;

    @Override
    @WithSpan("AzureDocumentIntelligenceAdapter.analyzeInvoice")
    @LogExecutionTime(value = LogExecutionTime.LogLevel.INFO, logParameters = false)
    public Uni<DocumentAnalysisResult> analyzeInvoice(byte[] documentBytes, String contentType) {
        log.info("Starting document analysis with Azure Document Intelligence (model: {}, contentType: {})",
                MODEL_ID, contentType);

        // Paso 1: Iniciar el análisis
        return initiateAnalysis(documentBytes, contentType)
                // Paso 2: Hacer polling del resultado
                .chain(operationLocation -> pollForResult(operationLocation))
                // Paso 3: Mapear a modelo de dominio
                .map(azureResponse -> {
                    log.info("Document analysis completed successfully");
                    return mapper.toDomain(azureResponse);
                })
                // Manejo de errores
                .onFailure().transform(this::mapException);
    }

    /**
     * Inicia el análisis del documento en Azure.
     * Retorna el resultId extraído del header Operation-Location.
     */
    private Uni<String> initiateAnalysis(byte[] documentBytes, String contentType) {
        log.info("Initiating document analysis with model: {}, API version: {}, contentType: {}",
                MODEL_ID, API_VERSION, contentType);

        return azureRestClient.analyzeDocument(
                        MODEL_ID,
                        azureApiKey,
                        contentType,
                        API_VERSION,
                        documentBytes
                )
                .onItem().transformToUni(response -> {
                    // Extraer el header Operation-Location
                    String operationLocation = response.getHeaderString("Operation-Location");

                    if (operationLocation == null || operationLocation.isEmpty()) {
                        log.error("Operation-Location header not found in Azure response");
                        return Uni.createFrom().failure(
                                new DocumentIntelligenceApiException(
                                        "Operation-Location header missing from Azure response"
                                )
                        );
                    }

                    log.debug("Analysis initiated. Operation location: {}", operationLocation);

                    // Extraer el resultId de la URL
                    // Formato: https://.../formrecognizer/documentModels/{modelId}/analyzeResults/{resultId}?...
                    String resultId = extractResultId(operationLocation);

                    if (resultId == null || resultId.isEmpty()) {
                        log.error("Failed to extract resultId from Operation-Location: {}", operationLocation);
                        return Uni.createFrom().failure(
                                new DocumentIntelligenceApiException(
                                        "Failed to extract resultId from Operation-Location"
                                )
                        );
                    }

                    log.debug("Extracted resultId: {}", resultId);
                    return Uni.createFrom().item(resultId);
                })
                .onFailure(WebApplicationException.class).transform(this::handleWebApplicationException);
    }

    /**
     * Extrae el resultId del header Operation-Location.
     * Formato esperado: https://.../formrecognizer/documentModels/{modelId}/analyzeResults/{resultId}?api-version=...
     */
    private String extractResultId(String operationLocation) {
        try {
            // Buscar "analyzeResults/" y extraer el resultId
            int startIndex = operationLocation.indexOf("/analyzeResults/");
            if (startIndex == -1) {
                return null;
            }

            startIndex += "/analyzeResults/".length();

            // El resultId va hasta el siguiente '?' o hasta el final
            int endIndex = operationLocation.indexOf('?', startIndex);
            if (endIndex == -1) {
                endIndex = operationLocation.length();
            }

            return operationLocation.substring(startIndex, endIndex);
        } catch (Exception e) {
            log.error("Error extracting resultId from Operation-Location", e);
            return null;
        }
    }

    /**
     * Hace polling del resultado del análisis hasta que esté completo.
     */
    private Uni<AzureAnalyzeResponse> pollForResult(String resultId) {
        log.debug("Starting polling for analysis result (resultId: {})...", resultId);
        return pollAttemptRecursive(resultId, 0);
    }

    /**
     * Realiza polling recursivo del resultado.
     */
    private Uni<AzureAnalyzeResponse> pollAttemptRecursive(String resultId, int attempt) {
        if (attempt >= MAX_POLLING_ATTEMPTS) {
            return Uni.createFrom().failure(
                    new DocumentIntelligenceApiException(
                            "Analysis timed out after " + MAX_POLLING_ATTEMPTS + " attempts"
                    )
            );
        }

        log.trace("Polling attempt {} of {}", attempt + 1, MAX_POLLING_ATTEMPTS);

        return azureRestClient.getAnalysisResult(MODEL_ID, resultId, azureApiKey, API_VERSION)
                .onFailure(WebApplicationException.class).transform(this::handleWebApplicationException)
                .chain(response -> {
                    String status = response.getStatus();

                    if ("succeeded".equalsIgnoreCase(status)) {
                        log.debug("Analysis completed successfully");
                        return Uni.createFrom().item(response);
                    }

                    if ("failed".equalsIgnoreCase(status)) {
                        String errorMsg = response.getError() != null
                                ? response.getError().getMessage()
                                : "Unknown error";
                        return Uni.createFrom().failure(
                                new DocumentIntelligenceApiException("Analysis failed: " + errorMsg)
                        );
                    }

                    // Status is "running" or "notStarted" - continue polling
                    log.trace("Analysis still in progress (status: {}), retrying in {} seconds",
                            status, POLLING_INTERVAL.getSeconds());
                    return Uni.createFrom().item(attempt + 1)
                            .onItem().delayIt().by(POLLING_INTERVAL)
                            .chain(nextAttempt -> pollAttemptRecursive(resultId, nextAttempt));
                });
    }

    /**
     * Maneja excepciones de WebApplicationException y las convierte a excepciones de dominio.
     */
    private Throwable handleWebApplicationException(Throwable throwable) {
        if (throwable instanceof WebApplicationException wae) {
            int status = wae.getResponse().getStatus();
            String errorBody = wae.getResponse().readEntity(String.class);

            log.error("Azure API error (status {}): {}", status, errorBody);

            return new DocumentIntelligenceApiException(
                    "Error calling Azure Document Intelligence API",
                    status,
                    extractErrorCode(errorBody)
            );
        }
        return throwable;
    }

    /**
     * Mapea excepciones genéricas a excepciones de dominio.
     */
    private Throwable mapException(Throwable throwable) {
        if (throwable instanceof DocumentIntelligenceApiException
                || throwable instanceof DocumentUnreadableException) {
            return throwable;
        }

        log.error("Unexpected error during document analysis", throwable);
        return new DocumentIntelligenceApiException(
                "Error inesperado al analizar el documento: " + throwable.getMessage(),
                throwable
        );
    }

    /**
     * Extrae el código de error del cuerpo de respuesta de error.
     */
    private String extractErrorCode(String errorBody) {
        // Implementación simple - podría mejorarse con Jackson
        if (errorBody != null && errorBody.contains("\"code\"")) {
            try {
                int codeStart = errorBody.indexOf("\"code\":\"") + 8;
                int codeEnd = errorBody.indexOf("\"", codeStart);
                return errorBody.substring(codeStart, codeEnd);
            } catch (Exception e) {
                log.debug("Could not extract error code from response body", e);
            }
        }
        return "UNKNOWN";
    }
}
