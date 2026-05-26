package org.walrex.infrastructure.adapter.outbound.screening;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.walrex.infrastructure.adapter.outbound.screening.dto.ElasticsearchResponse;
import org.walrex.infrastructure.adapter.outbound.screening.dto.ElasticsearchSearchRequest;

@Path("/sanctions_list")
@RegisterRestClient(configKey = "elasticsearch")
public interface ElasticsearchRestClient {

    @POST
    @Path("/_search")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Uni<ElasticsearchResponse> search(ElasticsearchSearchRequest request);
}
