package org.walrex.domain.service;

import lombok.extern.slf4j.Slf4j;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.walrex.application.dto.request.OtpGenerateRequest;
import org.walrex.application.dto.request.OtpValidateRequest;
import org.walrex.application.dto.response.OtpResponse;
import org.walrex.application.dto.response.OtpValidationResponse;
import org.walrex.application.port.input.OtpUseCase;
import org.walrex.application.port.output.OtpRepositoryPort;
import org.walrex.application.port.output.OutboxRepositoryPort;
import org.walrex.application.port.output.RegistrationTokenPort;
import org.walrex.domain.exception.InvalidOtpException;
import org.walrex.domain.factory.OtpFactory;
import org.walrex.domain.factory.OutboxEventFactory;
import org.walrex.domain.model.Otp;

@Slf4j
@ApplicationScoped
public class OtpDomainService implements OtpUseCase {

    @Inject
    OtpRepositoryPort otpRepositoryPort;

    @Inject
    OutboxRepositoryPort outboxRepositoryPort;

    @Inject
    RegistrationTokenPort registrationTokenPort;

    @Inject
    OtpGenerator otpGenerator;

    @Inject
    OtpHasher otpHasher;


    @Override
    @WithTransaction
    public Uni<OtpResponse> generateOtp(OtpGenerateRequest request) {
        log.debug("Generating OTP for request: {}", request);
        // Idempotencia: verificar si ya existe un OTP activo para este target y purpose
        return otpRepositoryPort.findActiveByTargetAndPurpose(
                        request.getTarget(),
                        request.getPurpose()
                )
                .flatMap(existingOtp -> {
                    if (existingOtp != null) {
                        // Ya existe un OTP activo, retornar el mismo sin crear nuevo
                        return Uni.createFrom().item(new OtpResponse(
                                existingOtp.getReferenceId(),
                                existingOtp.getExpiresAt()
                        ));
                    }
                    // No existe OTP activo, crear uno nuevo
                    return createNewOtp(request);
                });
    }

    private Uni<OtpResponse> createNewOtp(OtpGenerateRequest request) {
        String rawOtp = otpGenerator.generate();
        String hash = otpHasher.hash(rawOtp);

        Otp otp = OtpFactory.create(
                request.getTarget(),
                hash,
                request.getPurpose()
        );

        return otpRepositoryPort.save(otp)
                .flatMap(savedOtp -> {
                    var outboxEvent = OutboxEventFactory.createOtpSendEvent(
                            savedOtp,
                            rawOtp,
                            request.getChannel(),
                            request.getPurpose()
                    );
                    return outboxRepositoryPort.save(outboxEvent)
                            .replaceWith(savedOtp);
                })
                .map(saved ->
                        new OtpResponse(
                                saved.getReferenceId(),
                                saved.getExpiresAt()
                        )
                );
    }

    @Override
    @WithTransaction
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
                    return otpRepositoryPort.update(otp)
                            .replaceWith(otp);
                })
                .map(otp -> {
                    var token = registrationTokenPort.generate(
                            otp.getTarget(),
                            request.getPurpose().name()
                    );
                    return OtpValidationResponse.builder()
                            .valid(true)
                            .target(otp.getTarget())
                            .registrationToken(token.token())
                            .tokenExpiresAt(token.expiresAt())
                            .build();
                });
    }
}
