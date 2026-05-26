package org.walrex.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Builder
@AllArgsConstructor
public class MarketPrice {

    private final String symbol;
    private final BigDecimal price;
    private final Instant timestamp;
}
