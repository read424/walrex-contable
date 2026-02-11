package org.walrex.infrastructure.adapter.inbound.rest;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.walrex.application.dto.response.CountriesListResponse;
import org.walrex.application.dto.response.CountryRatesResponse;
import org.walrex.application.dto.response.CountryRateTypesResponse;
import org.walrex.application.dto.response.RateCalculationResponse;
import org.walrex.application.dto.request.CalculateRateRequest;
import org.walrex.application.port.input.GetCountryExchangeRatesUseCase;
import org.walrex.application.port.input.GetRemittanceCountriesUseCase;
import org.walrex.application.port.input.GetCountryRateTypesUseCase;
import org.walrex.application.port.input.CalculateRateTypeUseCase;

import java.util.Map;

@Path("/api/v1/exchange")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Remittance Exchange Rates", description = "API para tasas de cambio de remesas")
@RequiredArgsConstructor
public class RemittanceExchangeRateController {

    @Inject
    GetRemittanceCountriesUseCase getRemittanceCountriesUseCase;

    @Inject
    GetCountryExchangeRatesUseCase getCountryExchangeRatesUseCase;

    @Inject
    GetCountryRateTypesUseCase getCountryRateTypesUseCase;

    @Inject
    CalculateRateTypeUseCase calculateRateTypeUseCase;

    @GET
    @Path("/countries")
    @Operation(
        summary = "Obtener países disponibles para remesas",
        description = "Retorna la lista de países que tienen monedas operativas para remesas"
    )
    @APIResponse(
        responseCode = "200",
        description = "Lista de países obtenida exitosamente",
        content = @Content(schema = @Schema(implementation = CountriesListResponse.class))
    )
    public Uni<Response> getAvailableCountries() {
        return getRemittanceCountriesUseCase.getAvailableCountries()
            .onItem().transform(response -> Response.ok(response).build())
            .onFailure().recoverWithItem(throwable -> 
                Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Error al obtener países: " + throwable.getMessage()))
                    .build()
            );
    }

    @GET
    @Path("/countries/{iso2}/rates")
    @Operation(
        summary = "Obtener tasas de cambio por país origen",
        description = "Retorna todas las tasas de cambio disponibles desde un país específico hacia otros países"
    )
    @APIResponse(
        responseCode = "200",
        description = "Tasas de cambio obtenidas exitosamente",
        content = @Content(schema = @Schema(implementation = CountryRatesResponse.class))
    )
    @APIResponse(
        responseCode = "404",
        description = "País no encontrado"
    )
    public Uni<Response> getCountryExchangeRates(@PathParam("iso2") String countryIso2) {
        return getCountryExchangeRatesUseCase.getExchangeRatesByCountry(countryIso2.toUpperCase())
            .onItem().transform(response -> Response.ok(response).build())
            .onFailure(IllegalArgumentException.class).recoverWithItem(throwable ->
                Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", throwable.getMessage()))
                    .build()
            )
            .onFailure().recoverWithItem(throwable ->
                Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Error al obtener tasas: " + throwable.getMessage()))
                    .build()
            );
    }

    @GET
    @Path("/countries/{iso2}/rate-types")
    @Operation(
        summary = "Obtener tipos de tasa de cambio por país",
        description = "Retorna los tipos de tasa de cambio disponibles para un país específico (BCV, Paralelo, etc.)"
    )
    @APIResponse(
        responseCode = "200",
        description = "Tipos de tasa obtenidos exitosamente",
        content = @Content(schema = @Schema(implementation = CountryRateTypesResponse.class))
    )
    @APIResponse(
        responseCode = "404",
        description = "País no encontrado"
    )
    public Uni<Response> getCountryRateTypes(@PathParam("iso2") String countryIso2) {
        return getCountryRateTypesUseCase.getRateTypesByCountry(countryIso2.toUpperCase())
            .onItem().transform(response -> Response.ok(response).build())
            .onFailure(IllegalArgumentException.class).recoverWithItem(throwable ->
                Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", throwable.getMessage()))
                    .build()
            )
            .onFailure().recoverWithItem(throwable ->
                Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Error al obtener tipos de tasa: " + throwable.getMessage()))
                    .build()
            );
    }

    @POST
    @Path("/calculate-rate")
    @Operation(
        summary = "Calcular tasa de cambio",
        description = "Calcula el monto equivalente usando un tipo de tasa específico"
    )
    @APIResponse(
        responseCode = "200",
        description = "Cálculo realizado exitosamente",
        content = @Content(schema = @Schema(implementation = RateCalculationResponse.class))
    )
    @APIResponse(
        responseCode = "400",
        description = "Datos de entrada inválidos"
    )
    @APIResponse(
        responseCode = "404",
        description = "País o tipo de tasa no encontrado"
    )
    public Uni<Response> calculateRate(CalculateRateRequest request) {
        return calculateRateTypeUseCase.calculateRate(request)
            .onItem().transform(response -> Response.ok(response).build())
            .onFailure(IllegalArgumentException.class).recoverWithItem(throwable ->
                Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", throwable.getMessage()))
                    .build()
            )
            .onFailure().recoverWithItem(throwable ->
                Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Error al calcular tasa: " + throwable.getMessage()))
                    .build()
            );
    }
}