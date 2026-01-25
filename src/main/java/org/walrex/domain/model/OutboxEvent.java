package org.walrex.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OutboxEvent {
    private Long id;

    private String aggregateType;
    private String aggregateId;
    private String eventType;

    private String payload;

    private OutboxStatus status;

    private Instant createdAt;
    private Instant processedAt;
    private String lastError;

    @Builder.Default
    private Integer retryCount = 0;

    @Builder.Default
    private Integer maxRetries = 3;

    public boolean hasRetriesLeft() {
        return retryCount < maxRetries;
    }

    public void incrementRetry() {
        this.retryCount++;
    }
}
