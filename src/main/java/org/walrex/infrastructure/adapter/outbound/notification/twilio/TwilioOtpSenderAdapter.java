package org.walrex.infrastructure.adapter.outbound.notification.twilio;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;
import org.walrex.application.port.output.OtpSenderPort;
import org.walrex.domain.model.OtpChannel;
import org.walrex.domain.model.OtpPurpose;
import org.walrex.infrastructure.config.qualifier.Sms;
import org.walrex.infrastructure.config.qualifier.WhatsApp;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@ApplicationScoped
@Sms
@WhatsApp
public class TwilioOtpSenderAdapter implements OtpSenderPort {

    private static final Logger LOG = Logger.getLogger(TwilioOtpSenderAdapter.class);

    @Inject
    @RestClient
    TwilioRestClient twilioRestClient;

    @ConfigProperty(name = "app.twilio.account-sid")
    String accountSid;

    @ConfigProperty(name = "app.twilio.auth-token")
    String authToken;

    @ConfigProperty(name = "app.twilio.from-number")
    String fromNumber;

    @ConfigProperty(name = "app.twilio.whatsapp-from")
    String whatsappFrom;

    @Override
    public Uni<Void> sendOtp(OtpChannel channel, String target, String otp, OtpPurpose purpose) {
        if (channel != OtpChannel.SMS && channel != OtpChannel.WHATSAPP) {
            return Uni.createFrom().voidItem();
        }

        String from = fromNumber;
        String to = target;

        if (channel == OtpChannel.WHATSAPP) {
            from = whatsappFrom.startsWith("whatsapp:") ? whatsappFrom : "whatsapp:" + whatsappFrom;
            to = target.startsWith("whatsapp:") ? target : "whatsapp:" + target;
        }
        String body = getMessageBody(otp, purpose);

        String authHeader = "Basic " + Base64.getEncoder().encodeToString(
                (accountSid + ":" + authToken).getBytes(StandardCharsets.UTF_8)
        );

        LOG.infof("Sending OTP via %s to %s", channel, target);

        return twilioRestClient.sendMessage(accountSid, authHeader, to, from, body)
                .onItem().invoke(response -> LOG.infof("Twilio response: %s", response))
                .onFailure().invoke(e -> LOG.errorf(e, "Failed to send OTP via Twilio to %s", target))
                .replaceWithVoid();
    }

    private String getMessageBody(String otp, OtpPurpose purpose) {
        String purposeText = switch (purpose) {
            case REGISTER -> "confirmar tu registro";
            case LOGIN -> "iniciar sesión";
            case PASSWORD_RESET -> "restablecer tu contraseña";
            case MFA_SETUP -> "configurar tu verificación en dos pasos";
        };
        return String.format("Tu código de Walrex para %s es: %s. No lo compartas con nadie.", purposeText, otp);
    }
}
