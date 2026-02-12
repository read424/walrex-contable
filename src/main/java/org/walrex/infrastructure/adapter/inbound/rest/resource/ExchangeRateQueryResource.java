package org.walrex.infrastructure.adapter.inbound.rest.resource;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.walrex.application.dto.response.ErrorResponse;
import org.walrex.application.dto.response.ExchangeRateQueryResponse;
import org.walrex.application.port.input.GetExchangeRateUseCase;
import org.walrex.infrastructure.adapter.inbound.rest.security.JwtSecurityInterceptor;

@Slf4j
@Path("/api/v1/exchange/rate")
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Tag(name = "Tasas de Cambio", description = "Consulta de tasas de cambio almacenadas")
public class ExchangeRateQueryResource {

    @Inject
    GetExchangeRateUseCase getExchangeRateUseCase;

    @Inject
    JwtSecurityInterceptor jwtSecurityInterceptor;

    @GET
    @Operation(
            summary = "Consultar tasa de cambio",
            description = "Consulta la tasa de cambio almacenada entre dos paises/monedas. " +
                    "Busca primero en cache Redis, luego en base de datos."
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "Tasa encontrada",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = ExchangeRateQueryResponse.class)
                    )
            ),
            @APIResponse(
                    responseCode = "400",
                    description = "Par de monedas no encontrado o parametros invalidos",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @APIResponse(
                    responseCode = "500",
                    description = "Error interno del servidor",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    public Uni<Response> getExchangeRate(
            @Parameter(description = "Codigo ISO2 del pais origen (ej: PE)", required = true)
            @QueryParam("fromCountry") String fromCountry,
            @Parameter(description = "Codigo ISO3 de la moneda origen (ej: PEN)", required = true)
            @QueryParam("fromCurrency") String fromCurrency,
            @Parameter(description = "Codigo ISO2 del pais destino (ej: VE)", required = true)
            @QueryParam("toCountry") String toCountry,
            @Parameter(description = "Codigo ISO3 de la moneda destino (ej: VES)", required = true)
            @QueryParam("toCurrency") String toCurrency) {

        // TODO: Descomentar cuando se habilite autenticacion
        // Optional<Integer> userId = jwtSecurityInterceptor.authenticateAndGetUserId(authHeader);
        // if (userId.isEmpty()) {
        //     return Uni.createFrom().item(Response.status(Response.Status.UNAUTHORIZED)
        //             .entity(new ErrorResponse(401, "Unauthorized", "Invalid or missing token"))
        //             .build());
        // }

        log.info("Received exchange rate query: {}:{} -> {}:{}",
                fromCountry, fromCurrency, toCountry, toCurrency);

        if (fromCountry == null || fromCurrency == null || toCountry == null || toCurrency == null) {
            return Uni.createFrom().item(Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(400, "Bad Request",
                            "All query parameters are required: fromCountry, fromCurrency, toCountry, toCurrency"))
                    .build());
        }

        return getExchangeRateUseCase.getRate(
                        fromCountry.toUpperCase(), fromCurrency.toUpperCase(),
                        toCountry.toUpperCase(), toCurrency.toUpperCase())
                .map(response -> Response.ok(response).build())
                .onFailure()
                .recoverWithItem(this::mapExceptionToResponse);
    }

    private Response mapExceptionToResponse(Throwable throwable) {
        log.error("Error querying exchange rate", throwable);

        if (throwable instanceof IllegalArgumentException) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(
                            Response.Status.BAD_REQUEST.getStatusCode(),
                            Response.Status.BAD_REQUEST.getReasonPhrase(),
                            throwable.getMessage()))
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
