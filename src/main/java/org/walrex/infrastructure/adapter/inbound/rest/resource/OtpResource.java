package org.walrex.infrastructure.adapter.inbound.rest.resource;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.walrex.application.dto.request.OtpGenerateRequest;
import org.walrex.application.dto.request.OtpValidateRequest;
import org.walrex.application.dto.response.OtpResponse;
import org.walrex.application.dto.response.OtpValidationResponse;
import org.walrex.application.port.input.OtpUseCase;

@Path("/otp")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class OtpResource {

    @Inject
    OtpUseCase otpUseCase;

    @POST
    @Path("/generate")
    public Uni<OtpResponse> generate(OtpGenerateRequest request) {
        return otpUseCase.generateOtp(request);
    }

    @POST
    @Path("/validate")
    public Uni<OtpValidationResponse> validate(OtpValidateRequest request) {
        return otpUseCase.validateOtp(request);
    }
}
