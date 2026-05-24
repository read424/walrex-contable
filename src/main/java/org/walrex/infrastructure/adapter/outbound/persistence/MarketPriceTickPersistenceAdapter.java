package org.walrex.infrastructure.adapter.outbound.persistence;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.reactive.mutiny.Mutiny;
import org.walrex.application.port.output.MarketPriceTickPort;
import org.walrex.domain.model.MarketPriceTick;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Slf4j
@ApplicationScoped
public class MarketPriceTickPersistenceAdapter implements MarketPriceTickPort {

    @Inject
    Mutiny.SessionFactory sessionFactory;

    @Override
    public Uni<Void> record(MarketPriceTick tick) {
        OffsetDateTime recordedAt = tick.getRecordedAt() != null
                ? OffsetDateTime.ofInstant(tick.getRecordedAt(), ZoneOffset.UTC)
                : OffsetDateTime.now(ZoneOffset.UTC);

        return sessionFactory.withTransaction((session, tx) ->
            session.createNativeQuery(
                "INSERT INTO market_price_tick " +
                "(provider, symbol, currency_base, currency_quote, price, event_type, change_pct, recorded_at) " +
                "VALUES (:provider, :symbol, :base, :quote, :price, :eventType, :changePct, :recordedAt)")
                .setParameter("provider",   tick.getProvider())
                .setParameter("symbol",     tick.getSymbol())
                .setParameter("base",       tick.getCurrencyBase())
                .setParameter("quote",      tick.getCurrencyQuote())
                .setParameter("price",      tick.getPrice())
                .setParameter("eventType",  tick.getEventType())
                .setParameter("changePct",  tick.getChangePct())
                .setParameter("recordedAt", recordedAt)
                .executeUpdate()
                .replaceWithVoid()
        ).onFailure().invoke(e ->
                log.error("[MarketPriceTick] Error recording tick for {}: {}", tick.getSymbol(), e.getMessage())
        );
    }
}
