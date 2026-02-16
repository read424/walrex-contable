package org.walrex.infrastructure.adapter.inbound.rest.resource;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.walrex.application.dto.response.FinancialInstitutionResponse;
import org.walrex.application.port.input.GetFinancialInstitutionsUseCase;

import java.util.List;

@Path("/api/v1/financial-institutions")
@Tag(name = "Financial Institutions", description = "Operations related to financial institutions and payout methods")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class FinancialInstitutionResource {

    @Inject
    GetFinancialInstitutionsUseCase getFinancialInstitutionsUseCase;

    @GET
    @WithSession
    @Operation(summary = "Get financial institutions", description = "Retrieve a list of financial institutions filtered by payout method and country")
    @APIResponse(responseCode = "200", description = "Financial institutions retrieved successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = FinancialInstitutionResponse.class, type = SchemaType.ARRAY)))
    @APIResponse(responseCode = "500", description = "Internal server error")
    public Uni<Response> getFinancialInstitutions(
            @Parameter(description = "Method type (BANK, PAGO_MOVIL, WALLET)", required = true) @QueryParam("methodType") String methodType,
            @Parameter(description = "Country ISO2 code (e.g., VE, PE)", required = true) @QueryParam("countryIso2") String countryIso2) {
        
        if (methodType == null || countryIso2 == null) {
            return Uni.createFrom().item(Response.status(Response.Status.BAD_REQUEST)
                    .entity("Missing required parameters: methodType or countryIso2")
                    .build());
        }

        return getFinancialInstitutionsUseCase.getByMethodAndCountry(methodType, countryIso2)
                .map(list -> Response.ok(list).build());
    }
}
