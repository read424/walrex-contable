package org.walrex.domain.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Builder
public class SubscriptionState {

    private final String symbol;
    @Setter private boolean active;
    @Setter private BigDecimal lastPrice;
    @Setter private Instant lastUpdated;
    private final Instant subscribedAt;
}
