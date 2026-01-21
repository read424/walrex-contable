package org.walrex.domain.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.walrex.application.dto.request.OtpGenerateRequest;
import org.walrex.application.dto.request.OtpValidateRequest;
import org.walrex.application.dto.response.OtpResponse;
import org.walrex.application.dto.response.OtpValidationResponse;
import org.walrex.application.port.input.OtpUseCase;
import org.walrex.application.port.output.OtpRepositoryPort;
import org.walrex.application.port.output.OtpSenderPort;
import org.walrex.domain.exception.InvalidOtpException;
import org.walrex.domain.factory.OtpFactory;
import org.walrex.domain.model.Otp;

@ApplicationScoped
public class OtpDomainService implements OtpUseCase {

    private final OtpRepositoryPort otpRepositoryPort;
    private final OtpGenerator otpGenerator;
    private final OtpHasher otpHasher;
    private final OtpSenderPort otpSenderPort;

    @Inject
    public OtpDomainService(
            OtpRepositoryPort repository,
            OtpGenerator otpGenerator,
            OtpHasher otpHasher,
            OtpSenderPort otpSenderPort
    ) {
        this.otpRepositoryPort = repository;
        this.otpGenerator = otpGenerator;
        this.otpHasher = otpHasher;
        this.otpSenderPort = otpSenderPort;
    }

    @Override
    public Uni<OtpResponse> generateOtp(OtpGenerateRequest request) {

        String rawOtp = otpGenerator.generate();
        String hash = otpHasher.hash(rawOtp);

        Otp otp = OtpFactory.create(
                request.getTarget(),
                hash,
                request.getPurpose()
        );

        return otpRepositoryPort.save(otp)
                .call(saved->
                                otpSenderPort.sendOtp(
                                        request.getChannel(),
                                        request.getTarget(),
                                        rawOtp,
                                        request.getPurpose()
                                )
                )
                .map(saved ->
                        new OtpResponse(
                                saved.getReferenceId(),
                                saved.getExpiresAt()
                        )
                );
    }

    @Override
    public Uni<OtpValidationResponse> validateOtp(OtpValidateRequest request) {

        return otpRepositoryPort.findValidOtp(
                        request.getReferenceId(),
                        request.getPurpose()
                )
                .onItem().ifNull().failWith(InvalidOtpException::new)
                .flatMap(otp -> {
                    if (otp.isExpired()
                            || !otp.matches(request.getCode(), otpHasher)) {
                        return Uni.createFrom()
                                .failure(new InvalidOtpException("Security Code OTP is Invalidate or Expired"));
                    }

                    otp.markAsUsed();
                    return otpRepositoryPort.update(otp);
                })
                .replaceWith(new OtpValidationResponse(true));
    }
}
