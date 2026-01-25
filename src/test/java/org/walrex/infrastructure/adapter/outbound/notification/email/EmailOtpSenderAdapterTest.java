package org.walrex.infrastructure.adapter.outbound.notification.email;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.reactive.ReactiveMailer;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;
import io.smallrye.mutiny.Uni;
import org.walrex.RandomPortTestProfile;

import jakarta.inject.Inject;
import java.time.Duration;

@QuarkusTest
@TestProfile(RandomPortTestProfile.class)
public class EmailOtpSenderAdapterTest {

    @Inject
    ReactiveMailer mailer;

    @Test
    void testSendEmail() {
        // Create a simple email
        Mail mail = Mail.withText("developer@texlamerced.com",
                "Quarkus Mailer Test",
                "This is a test email from the Quarkus application."
        );

        // Send the email and wait for the result
        Uni<Void> send = mailer.send(mail);
        try {
            send.await().atMost(Duration.ofSeconds(10));
        } catch (Exception e) {
            // If an exception is thrown, the test will fail
            throw new RuntimeException("Failed to send email", e);
        }
    }
}
