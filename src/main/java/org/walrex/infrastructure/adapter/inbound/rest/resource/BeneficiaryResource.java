package org.walrex.infrastructure.adapter.inbound.rest.resource;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.walrex.application.dto.query.BeneficiaryFilter;
import org.walrex.application.dto.query.PageRequest;
import org.walrex.application.dto.response.BeneficiaryResponse;
import org.walrex.application.dto.response.PagedResponse;
import org.walrex.application.port.input.ListBeneficiaryUseCase;
import org.walrex.application.port.input.GetBeneficiaryUseCase;
import org.walrex.infrastructure.adapter.inbound.mapper.BeneficiaryDtoMapper;
import org.walrex.infrastructure.adapter.inbound.rest.security.JwtSecurityInterceptor;

import java.util.Map;
import java.util.Optional;

@Path("/api/v1/beneficiaries")
@Tag(name = "Beneficiaries", description = "Operations related to beneficiaries")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BeneficiaryResource {

    @Inject
    ListBeneficiaryUseCase listUC;

    @Inject
    GetBeneficiaryUseCase getUC;

    @Inject
    BeneficiaryDtoMapper dtoMapper;

    @Inject
    JwtSecurityInterceptor jwtSecurityInterceptor;

    @GET
    @WithSession
    @Operation(summary = "List beneficiaries", description = "Retrieve a paginated list of beneficiaries with unified search and favorites filter. Client ID is extracted from JWT.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Beneficiaries retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = PagedResponse.class))),
            @APIResponse(responseCode = "401", description = "Unauthorized"),
            @APIResponse(responseCode = "500", description = "Internal server error")
    })
    public Uni<Response> list(
            @HeaderParam("Authorization") String authHeader,
            @Parameter(description = "Page number (1-based)", example = "1") @QueryParam("page") @DefaultValue("1") int page,
            @Parameter(description = "Page size", example = "20") @QueryParam("size") @DefaultValue("20") int size,
            @Parameter(description = "Unified search term") @QueryParam("search") String search,
            @Parameter(description = "Filter by favorites") @QueryParam("favorites") @DefaultValue("false") boolean favorites,
            @Parameter(description = "Sort by field", example = "id") @QueryParam("sortBy") @DefaultValue("id") String sortBy,
            @Parameter(description = "Sort direction (ASC/DESC)", example = "ASC") @QueryParam("sortDirection") @DefaultValue("ASC") String sortDirection) {

        Optional<Integer> clientIdOpt = jwtSecurityInterceptor.authenticateAndGetClientId(authHeader);
        if (clientIdOpt.isEmpty()) {
            return Uni.createFrom().item(Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("error", "Unauthorized", "message", "Missing or invalid token"))
                    .build());
        }

        Integer clientId = clientIdOpt.get();

        PageRequest pageRequest = PageRequest.builder()
                .page(Math.max(0, page - 1))
                .size(size)
                .sortBy(sortBy)
                .sortDirection(PageRequest.SortDirection.fromString(sortDirection))
                .build();

        BeneficiaryFilter filter = BeneficiaryFilter.builder()
                .clientId(clientId)
                .search(search)
                .favorites(favorites)
                .build();

        return listUC.list(pageRequest, filter)
                .map(pagedResponse -> Response.ok(pagedResponse).build());
    }

    @GET
    @Path("/{id}")
    @WithSession
    @Operation(summary = "Get beneficiary by ID", description = "Retrieve a single beneficiary by its ID")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Beneficiary found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = BeneficiaryResponse.class))),
            @APIResponse(responseCode = "404", description = "Beneficiary not found"),
            @APIResponse(responseCode = "500", description = "Internal server error")
    })
    public Uni<Response> getById(@PathParam("id") Long id) {
        return getUC.findById(id)
                .onItem().ifNotNull().transform(beneficiary -> Response.ok(dtoMapper.toResponse(beneficiary)).build())
                .onItem().ifNull().continueWith(() -> Response.status(Response.Status.NOT_FOUND).build());
    }
}
