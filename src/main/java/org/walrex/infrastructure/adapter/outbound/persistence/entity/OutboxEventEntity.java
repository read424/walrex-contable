package org.walrex.infrastructure.adapter.outbound.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.walrex.domain.model.OutboxStatus;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "outbox_event",
        indexes = {
                @Index(
                        name = "idx_outbox_status_created",
                        columnList = "status, created_at"
                ),
                @Index(
                        name = "idx_outbox_aggregate",
                        columnList = "aggregate_type, aggregate_id"
                ),
                @Index(
                        name = "uq_outbox_idempotency",
                        columnList = "aggregate_type, aggregate_id, event_type",
                        unique = true
                )
        }
    )
public class OutboxEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "aggregate_type", length = 32, nullable = false)
    private String aggregateType; // "OTP"

    @Column(name = "aggregate_id", length = 64, nullable = false)
    private String aggregateId; // referenceId

    @Column(name = "event_type", length = 64, nullable = false)
    private String eventType; // "SEND_OTP"

    @Column(name = "payload", columnDefinition = "jsonb", nullable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private OutboxStatus status; // PENDING, PROCESSING, SENT, FAILED

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "max_retries", nullable = false)
    @Builder.Default
    private Integer maxRetries = 3;
}
