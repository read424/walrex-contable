package org.walrex.infrastructure.adapter.outbound.astropay;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.walrex.infrastructure.adapter.outbound.astropay.dto.AstroPayExchangeRequest;
import org.walrex.infrastructure.adapter.outbound.astropay.dto.AstroPayExchangeResponse;

@Path("/v1/public/exchanges")
@RegisterRestClient(configKey = "astropay")
public interface AstroPayRestClient {

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Uni<AstroPayExchangeResponse> getExchangeRate(AstroPayExchangeRequest request);
}
