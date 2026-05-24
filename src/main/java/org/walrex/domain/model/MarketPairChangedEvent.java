package org.walrex.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MarketPairChangedEvent {

    private final String symbol;
    private final MarketPairAction action;
}
