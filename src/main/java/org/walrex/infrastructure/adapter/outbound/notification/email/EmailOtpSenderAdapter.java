package org.walrex.infrastructure.adapter.outbound.notification.email;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.reactive.ReactiveMailer;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.walrex.application.port.output.OtpSenderPort;
import org.walrex.domain.model.OtpChannel;
import org.walrex.domain.model.OtpPurpose;

@ApplicationScoped
public class EmailOtpSenderAdapter implements OtpSenderPort {

    @Inject
    ReactiveMailer mailer;

    @Inject
    @Location("otp-email")
    Template otpEmail;

    @Override
    public Uni<Void> sendOtp(
            OtpChannel channel,
            String target,
            String otp,
            OtpPurpose purpose
    ) {
        if (channel != OtpChannel.EMAIL) {
            return Uni.createFrom().voidItem();
        }

        String html = otpEmail
                .data("otp", otp)
                .data("purpose", purpose.name().replace("_", " ").toLowerCase())
                .data("expiresMinutes", expirationMinutes(purpose))
                .render();

        return mailer.send(
                Mail.withHtml(
                        target,
                        subjectByPurpose(purpose),
                        html
                )
        ).replaceWithVoid();
    }

    private String subjectByPurpose(OtpPurpose purpose) {
        return switch (purpose) {
            case REGISTER -> "Confirma tu registro";
            case LOGIN -> "Código de acceso";
            case PASSWORD_RESET -> "Restablecer contraseña";
            case MFA_SETUP -> "Configuración de verificación en dos pasos";
        };
    }

    private int expirationMinutes(OtpPurpose purpose) {
        return switch (purpose) {
            case LOGIN -> 3;
            case REGISTER -> 5;
            case PASSWORD_RESET -> 10;
            case MFA_SETUP -> 15;
        };
    }
}
