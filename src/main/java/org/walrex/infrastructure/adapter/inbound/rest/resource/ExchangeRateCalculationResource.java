package org.walrex.infrastructure.adapter.inbound.rest.resource;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.walrex.application.dto.request.ExchangeRateRequest;
import org.walrex.application.dto.response.ErrorResponse;
import org.walrex.application.dto.response.ExchangeRateResponse;
import org.walrex.application.port.input.CalculateExchangeRateUseCase;
import org.walrex.infrastructure.adapter.inbound.mapper.ExchangeRateMapper;

/**
 * REST Resource para cálculo de tasas de cambio entre divisas.
 *
 * Proporciona endpoint para conversiones en tiempo real usando Binance P2P.
 */
@Slf4j
@Path("/api/v1/exchange-rates")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Tag(name = "Tasas de Cambio", description = "Cálculo de conversión de divisas en tiempo real")
public class ExchangeRateCalculationResource {

    @Inject
    CalculateExchangeRateUseCase calculateExchangeRateUseCase;

    @Inject
    ExchangeRateMapper exchangeRateMapper;

    @POST
    @Operation(
            summary = "Calcular tasa de cambio",
            description = "Calcula la conversión entre dos divisas usando tasas en tiempo real de Binance P2P. " +
                    "Utiliza USDT como intermediario para conversiones cruzadas."
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "Cálculo exitoso",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = ExchangeRateResponse.class)
                    )
            ),
            @APIResponse(
                    responseCode = "400",
                    description = "Datos de entrada inválidos o no existe ruta configurada",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @APIResponse(
                    responseCode = "500",
                    description = "Error interno del servidor o error al consultar proveedor",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    public Uni<Response> calculateExchangeRate(@Valid ExchangeRateRequest request) {
        log.info("Received exchange rate calculation request: {} {} -> {}",
                request.amount(), request.baseCurrency(), request.quoteCurrency());

        return calculateExchangeRateUseCase.calculateExchangeRate(
                        request.amount(),
                        request.baseCurrency(),
                        request.quoteCurrency(),
                        request.margin()
                )
                .map(exchangeRateMapper::toResponse)
                .map(response -> Response.ok(response).build())
                .onFailure()
                .recoverWithItem(this::mapExceptionToResponse);
    }

    /**
     * Mapea excepciones a respuestas HTTP apropiadas.
     */
    private Response mapExceptionToResponse(Throwable throwable) {
        log.error("Error calculating exchange rate", throwable);

        if (throwable instanceof IllegalArgumentException) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(
                            Response.Status.BAD_REQUEST.getStatusCode(),
                            Response.Status.BAD_REQUEST.getReasonPhrase(),
                            throwable.getMessage()))
                    .build();
        }

        if (throwable instanceof IllegalStateException) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse(
                            Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                            Response.Status.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                            "Exchange rate provider error: " + throwable.getMessage()))
                    .build();
        }

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse(
                        Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                        Response.Status.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                        "An unexpected error occurred"))
                .build();
    }
}
