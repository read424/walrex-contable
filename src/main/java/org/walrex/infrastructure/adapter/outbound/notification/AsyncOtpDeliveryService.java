package org.walrex.infrastructure.adapter.outbound.notification;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.walrex.application.port.output.OtpSenderPort;
import org.walrex.domain.model.OtpChannel;
import org.walrex.domain.model.OtpPurpose;

@Slf4j
@ApplicationScoped
public class AsyncOtpDeliveryService {

    @Inject
    OtpSenderPort otpSenderPort;

    public void sendOtp(OtpChannel channel, String target, String otp, OtpPurpose purpose) {
        otpSenderPort.sendOtp(channel, target, otp, purpose)
                .subscribe().with(
                        ignored -> log.info("OTP dispatched asynchronously for purpose {} to {} via {}",
                                purpose, target, channel),
                        failure -> log.error("Failed to dispatch OTP asynchronously for purpose {} to {} via {}",
                                purpose, target, channel, failure)
                );
    }
}
