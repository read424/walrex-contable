package org.walrex.infrastructure.adapter.outbound.notification.twilio;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.RestForm;

@Path("/2010-04-01/Accounts/{AccountSid}")
@RegisterRestClient(configKey = "twilio-api")
public interface TwilioRestClient {

    @POST
    @Path("/Messages.json")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    Uni<String> sendMessage(
            @PathParam("AccountSid") String accountSid,
            @HeaderParam("Authorization") String authHeader,
            @RestForm("To") String to,
            @RestForm("From") String from,
            @RestForm("Body") String body
    );
}
