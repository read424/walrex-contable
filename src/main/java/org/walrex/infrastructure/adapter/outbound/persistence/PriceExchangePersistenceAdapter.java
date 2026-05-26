package org.walrex.infrastructure.adapter.outbound.persistence;

import io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.impl.ContextInternal;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.reactive.mutiny.Mutiny;
import org.walrex.application.port.output.PriceExchangeOutputPort;
import org.walrex.infrastructure.adapter.outbound.persistence.repository.PriceExchangeRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Adaptador de persistencia para tasas de cambio.
 */
@Slf4j
@ApplicationScoped
public class PriceExchangePersistenceAdapter implements PriceExchangeOutputPort {

    private static final String TYPE_OPERATION_REMESAS = "3";

    @Inject
    PriceExchangeRepository priceExchangeRepository;

    @Inject
    Mutiny.SessionFactory sessionFactory;

    @Inject
    Vertx vertx;

    @Override
    public Uni<Integer> saveAverageRate(
            String currencyBaseCode,
            String currencyQuoteCode,
            BigDecimal averagePrice,
            LocalDate exchangeDate) {

        log.warn("saveAverageRate with String codes is deprecated - use upsertRate with Integer IDs");
        return Uni.createFrom().failure(
                new UnsupportedOperationException("Use upsertRate with currency IDs instead")
        );
    }

    @Override
    public Uni<Integer> upsertRate(
            Integer currencyBaseId,
            Integer currencyQuoteId,
            BigDecimal averagePrice,
            LocalDate exchangeDate) {

        log.info("=== [UPSERT START] Thread: {} | base ID:{} -> quote ID:{} | price: {} | date: {} ===",
                Thread.currentThread().getName(), currencyBaseId, currencyQuoteId, averagePrice, exchangeDate);

        // Run in a fresh Vert.x context to prevent SlowContextualSupplier (MicroProfile CP) from
        // capturing a closed @WithSession session that may be present in the caller's context and
        // restoring it inside executeInTransaction, causing "Session/EntityManager is closed".
        return Uni.createFrom().<Integer>emitter(emitter -> {
            Context freshCtx = ((ContextInternal) vertx.getOrCreateContext()).duplicate();
            VertxContextSafetyToggle.setContextSafe(freshCtx, true);
            freshCtx.runOnContext(v ->
                sessionFactory.withTransaction((session, tx) ->
                    session.createMutationQuery(
                        "UPDATE PriceExchangeEntity SET isActive = '0' " +
                        "WHERE dateExchange = :date AND idCurrencyBase = :base " +
                        "AND idCurrencyQuote = :quote AND typeOperation = :type AND isActive = '1'"
                    )
                    .setParameter("date", exchangeDate)
                    .setParameter("base", currencyBaseId)
                    .setParameter("quote", currencyQuoteId)
                    .setParameter("type", TYPE_OPERATION_REMESAS)
                    .executeUpdate()
                    .invoke(count -> log.info(
                            "=== [DEACTIVATE] Thread: {} | Deactivated: {} | base:{} -> quote:{} | date: {} ===",
                            Thread.currentThread().getName(), count, currencyBaseId, currencyQuoteId, exchangeDate))
                    .chain(__ -> {
                        log.info("=== [CREATE] Thread: {} | Inserting: base:{} -> quote:{} = {} | date: {} ===",
                                Thread.currentThread().getName(), currencyBaseId, currencyQuoteId, averagePrice, exchangeDate);
                        return session.<Integer>createNativeQuery(
                                "INSERT INTO price_exchange " +
                                "(type_operation, id_currency_base, id_currency_quote, amount_price, date_exchange, is_active, created_at) " +
                                "VALUES (:type, :base, :quote, :price, :date, '1', NOW()) RETURNING id",
                                Integer.class)
                                .setParameter("type", TYPE_OPERATION_REMESAS)
                                .setParameter("base", currencyBaseId)
                                .setParameter("quote", currencyQuoteId)
                                .setParameter("price", averagePrice)
                                .setParameter("date", exchangeDate)
                                .getSingleResult()
                                .invoke(savedId -> log.info("=== [SAVED] Thread: {} | ID:{} | base:{} -> quote:{} = {} ===",
                                        Thread.currentThread().getName(), savedId, currencyBaseId, currencyQuoteId, averagePrice));
                    })
                )
                .onFailure().invoke(error ->
                        log.error("=== [ERROR] Thread: {} | Failed to upsert rate | base:{} -> quote:{} | error: {} ===",
                                Thread.currentThread().getName(), currencyBaseId, currencyQuoteId,
                                error.getMessage(), error)
                )
                .subscribe().with(emitter::complete, emitter::fail)
            );
        });
    }

    @Override
    public Uni<Optional<BigDecimal>> findActiveRateByCountriesAndCurrencies(
            String fromCountryCode, String fromCurrencyCode,
            String toCountryCode, String toCurrencyCode) {

        log.info("=== [QUERY] Finding active rate for {}:{} -> {}:{} ===",
                fromCountryCode, fromCurrencyCode, toCountryCode, toCurrencyCode);

        return priceExchangeRepository.findActiveRateByCountriesAndCurrencies(
                fromCountryCode, fromCurrencyCode, toCountryCode, toCurrencyCode
        ).onFailure().invoke(error ->
                log.error("=== [QUERY ERROR] Failed to find rate for {}:{} -> {}:{} | error: {} ===",
                        fromCountryCode, fromCurrencyCode, toCountryCode, toCurrencyCode, error.getMessage())
        );
    }
}
