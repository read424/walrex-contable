package org.walrex.infrastructure.adapter.outbound.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.scheduler.Scheduled;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.walrex.application.port.output.OtpSenderPort;
import org.walrex.application.port.output.OutboxRepositoryPort;
import org.walrex.domain.model.OtpChannel;
import org.walrex.domain.model.OtpPurpose;
import org.walrex.domain.model.OutboxEvent;

import java.util.Map;

@ApplicationScoped
public class OutboxProcessor {

    private static final Logger LOG = Logger.getLogger(OutboxProcessor.class);
    private static final int BATCH_SIZE = 10;

    @Inject
    OutboxRepositoryPort outboxRepository;

    @Inject
    OtpSenderPort otpSenderPort;

    @Inject
    ObjectMapper objectMapper;

    @Scheduled(every = "5s", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    public Uni<Void> process() {
        return Panache.withTransaction(() ->
                outboxRepository.lockPending(BATCH_SIZE)
                        .onItem().transformToMulti(events -> Multi.createFrom().iterable(events))
                        .onItem().transformToUniAndConcatenate(this::handleEvent)
                        .collect().asList()
                        .replaceWithVoid()
        ).onFailure().invoke(failure ->
                LOG.error("Error processing outbox batch", failure)
        ).replaceWithVoid();
    }

    private Uni<Void> handleEvent(OutboxEvent event) {
        return parseAndSend(event)
                .call(() -> outboxRepository.markAsSent(event.getId()))
                .onFailure().recoverWithUni(err -> {
                    LOG.errorf("Failed to process outbox event %d: %s", event.getId(), err.getMessage());
                    return outboxRepository.markAsFailed(event.getId(), truncateError(err.getMessage()));
                });
    }

    private Uni<Void> parseAndSend(OutboxEvent event) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, String> payload = objectMapper.readValue(event.getPayload(), Map.class);

            return otpSenderPort.sendOtp(
                    OtpChannel.valueOf(payload.get("channel")),
                    payload.get("target"),
                    payload.get("otp"),
                    OtpPurpose.valueOf(payload.get("purpose"))
            );
        } catch (Exception e) {
            LOG.errorf("Failed to parse outbox event payload for event %d: %s", event.getId(), e.getMessage());
            return Uni.createFrom().failure(
                    new IllegalStateException("Invalid payload format: " + e.getMessage(), e)
            );
        }
    }

    private String truncateError(String message) {
        if (message == null) {
            return "Unknown error";
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }
}
