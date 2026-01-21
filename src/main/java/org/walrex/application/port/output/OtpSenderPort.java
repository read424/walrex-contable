package org.walrex.application.port.output;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.OtpChannel;
import org.walrex.domain.model.OtpPurpose;

public interface OtpSenderPort {
    Uni<Void> sendOtp(
            OtpChannel channel,
            String target,
            String otp,
            OtpPurpose purpose
    );
}
