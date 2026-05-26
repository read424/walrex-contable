package org.walrex.application.port.output;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.MarketPrice;

import java.time.Duration;
import java.util.Optional;

public interface MarketPriceCachePort {

    Uni<Optional<MarketPrice>> get(String symbol);

    Uni<Void> set(String symbol, MarketPrice price, Duration ttl);

    Uni<Boolean> delete(String symbol);
}
