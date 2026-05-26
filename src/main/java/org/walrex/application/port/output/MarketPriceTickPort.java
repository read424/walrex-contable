package org.walrex.application.port.output;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.MarketPriceTick;

public interface MarketPriceTickPort {
    Uni<Void> record(MarketPriceTick tick);
}
