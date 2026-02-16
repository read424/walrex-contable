package org.walrex.infrastructure.adapter.outbound.notification;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.walrex.application.port.output.OtpSenderPort;
import org.walrex.domain.model.OtpChannel;
import org.walrex.domain.model.OtpPurpose;
import org.walrex.infrastructure.config.qualifier.Email;
import org.walrex.infrastructure.config.qualifier.Sms;
import org.walrex.infrastructure.config.qualifier.WhatsApp;

@ApplicationScoped
public class OtpSenderDispatcher implements OtpSenderPort {

    @Inject
    @Email
    OtpSenderPort emailSender;

    @Inject
    @Sms
    OtpSenderPort smsSender;

    @Inject
    @WhatsApp
    OtpSenderPort whatsappSender;

    @Override
    public Uni<Void> sendOtp(OtpChannel channel, String target, String otp, OtpPurpose purpose) {
        if (channel == OtpChannel.EMAIL) {
            return emailSender.sendOtp(channel, target, otp, purpose);
        } else if (channel == OtpChannel.SMS) {
            return smsSender.sendOtp(channel, target, otp, purpose);
        } else if (channel == OtpChannel.WHATSAPP) {
            return whatsappSender.sendOtp(channel, target, otp, purpose);
        }
        return Uni.createFrom().voidItem();
    }
}
