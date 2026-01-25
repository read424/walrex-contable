package org.walrex.domain.factory;

import org.walrex.domain.model.Otp;
import org.walrex.domain.model.OtpChannel;
import org.walrex.domain.model.OtpPurpose;
import org.walrex.domain.model.OutboxEvent;
import org.walrex.domain.model.OutboxStatus;

import java.time.Instant;

public final class OutboxEventFactory {

    private static final String AGGREGATE_TYPE_OTP = "OTP";
    private static final String EVENT_TYPE_SEND_OTP = "SEND_OTP";

    private OutboxEventFactory() {}

    public static OutboxEvent createOtpSendEvent(
            Otp otp,
            String rawOtp,
            OtpChannel channel,
            OtpPurpose purpose
    ) {
        String payload = buildOtpPayload(otp.getTarget(), rawOtp, channel, purpose);

        return OutboxEvent.builder()
                .aggregateType(AGGREGATE_TYPE_OTP)
                .aggregateId(otp.getReferenceId())
                .eventType(EVENT_TYPE_SEND_OTP)
                .payload(payload)
                .status(OutboxStatus.PENDING)
                .createdAt(Instant.now())
                .build();
    }

    private static String buildOtpPayload(
            String target,
            String rawOtp,
            OtpChannel channel,
            OtpPurpose purpose
    ) {
        return String.format(
                "{\"channel\":\"%s\",\"target\":\"%s\",\"otp\":\"%s\",\"purpose\":\"%s\"}",
                channel.name(),
                target,
                rawOtp,
                purpose.name()
        );
    }
}
