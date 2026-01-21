package org.walrex.infrastructure.adapter.outbound.facematch;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.MultipartForm;

/**
 * REST Client para el servicio de comparación facial en Python.
 */
@Path("/face")
@RegisterRestClient(configKey = "face-match-service")
public interface FaceMatchRestClient {

    @POST
    @Path("/compare")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    Uni<FaceCompareResponse> compareFaces(@MultipartForm FaceCompareForm form);
}
