package org.walrex.infrastructure.adapter.outbound.notification.push;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.SendResponse;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.walrex.application.port.output.DeviceTokenRepositoryPort;
import org.walrex.application.port.output.PushNotificationPort;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@ApplicationScoped
public class FirebaseCloudMessagingAdapter implements PushNotificationPort {

    @ConfigProperty(name = "fcm.enabled", defaultValue = "false")
    boolean fcmEnabled;

    @ConfigProperty(name = "fcm.credentials-file", defaultValue = "firebase-service-account.json")
    String credentialsFile;

    @Inject
    DeviceTokenRepositoryPort deviceTokenRepositoryPort;

    private FirebaseMessaging firebaseMessaging;

    @PostConstruct
    void init() {
        if (!fcmEnabled) {
            log.warn("FCM is disabled. Push notifications will not be sent.");
            return;
        }

        try (InputStream serviceAccount = Thread.currentThread()
                .getContextClassLoader().getResourceAsStream(credentialsFile)) {
            if (serviceAccount == null) {
                log.error("FCM credentials file not found in classpath: {}", credentialsFile);
                return;
            }
            GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccount);
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }
            firebaseMessaging = FirebaseMessaging.getInstance();
            log.info("Firebase Cloud Messaging initialized successfully");
        } catch (IOException e) {
            log.error("Failed to initialize Firebase Cloud Messaging: {}", e.getMessage());
        }
    }

    @Override
    public Uni<Void> sendToDevice(String deviceToken, Map<String, String> data) {
        if (!fcmEnabled || firebaseMessaging == null) {
            log.warn("FCM is not available. Skipping push to device.");
            return Uni.createFrom().voidItem();
        }

        return Uni.createFrom().<Void>emitter(em -> {
            try {
                Message message = Message.builder()
                        .setToken(deviceToken)
                        .putAllData(data)
                        .build();

                String messageId = firebaseMessaging.send(message);
                log.debug("FCM message sent successfully. messageId: {}", messageId);
                em.complete(null);
            } catch (FirebaseMessagingException e) {
                handleMessagingError(deviceToken, e);
                em.complete(null);
            }
        }).runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    @Override
    public Uni<Void> sendToAllActiveDevices(Map<String, String> data) {
        if (!fcmEnabled || firebaseMessaging == null) {
            log.warn("FCM is not available. Skipping broadcast push.");
            return Uni.createFrom().voidItem();
        }

        return deviceTokenRepositoryPort.findAllActive()
                .onItem().transformToUni(tokens -> {
                    if (tokens.isEmpty()) {
                        log.info("No active device tokens found. Skipping FCM broadcast.");
                        return Uni.createFrom().voidItem();
                    }

                    List<String> tokenStrings = tokens.stream()
                            .map(dt -> dt.getToken())
                            .toList();

                    log.info("Sending FCM multicast to {} active devices", tokenStrings.size());
                    return sendMulticast(tokenStrings, data);
                });
    }

    private Uni<Void> sendMulticast(List<String> tokens, Map<String, String> data) {
        int batchSize = 500;
        List<List<String>> batches = new ArrayList<>();
        for (int i = 0; i < tokens.size(); i += batchSize) {
            batches.add(tokens.subList(i, Math.min(i + batchSize, tokens.size())));
        }

        return Uni.createFrom().<Void>emitter(em -> {
            for (List<String> batch : batches) {
                try {
                    MulticastMessage message = MulticastMessage.builder()
                            .addAllTokens(batch)
                            .putAllData(data)
                            .build();

                    BatchResponse response = firebaseMessaging.sendEachForMulticast(message);
                    log.info("FCM multicast batch: {} success, {} failures",
                            response.getSuccessCount(), response.getFailureCount());

                    if (response.getFailureCount() > 0) {
                        processMulticastErrors(batch, response);
                    }
                } catch (FirebaseMessagingException e) {
                    log.error("FCM multicast batch failed: {}", e.getMessage());
                }
            }
            em.complete(null);
        }).runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    private void processMulticastErrors(List<String> tokens, BatchResponse response) {
        List<SendResponse> responses = response.getResponses();
        for (int i = 0; i < responses.size(); i++) {
            if (!responses.get(i).isSuccessful()) {
                FirebaseMessagingException ex = responses.get(i).getException();
                if (ex != null) {
                    handleMessagingError(tokens.get(i), ex);
                }
            }
        }
    }

    private void handleMessagingError(String deviceToken, FirebaseMessagingException e) {
        MessagingErrorCode errorCode = e.getMessagingErrorCode();
        if (errorCode == MessagingErrorCode.UNREGISTERED || errorCode == MessagingErrorCode.INVALID_ARGUMENT) {
            log.warn("FCM token invalid or unregistered. Deactivating token: {}", deviceToken);
            deviceTokenRepositoryPort.deactivate(deviceToken)
                    .subscribe().with(
                            v -> log.info("Deactivated invalid FCM token: {}", deviceToken),
                            err -> log.error("Failed to deactivate FCM token: {}", err.getMessage())
                    );
        } else {
            log.error("FCM send failed for token {}: {} (code: {})",
                    deviceToken, e.getMessage(), errorCode);
        }
    }
}
