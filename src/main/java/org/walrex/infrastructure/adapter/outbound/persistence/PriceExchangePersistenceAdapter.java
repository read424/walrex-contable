package org.walrex.infrastructure.adapter.outbound.persistence;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.walrex.application.port.output.PriceExchangeOutputPort;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.PriceExchangeEntity;
import org.walrex.infrastructure.adapter.outbound.persistence.repository.PriceExchangeRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Adaptador de persistencia para tasas de cambio.
 *
 * Usa Panache.withTransaction() programáticamente en vez de @WithTransaction
 * para garantizar que cada llamada secuencial obtenga su propia sesión/transacción,
 * evitando "No current Mutiny.Session found" al encadenar múltiples upserts.
 */
@Slf4j
@ApplicationScoped
public class PriceExchangePersistenceAdapter implements PriceExchangeOutputPort {

    private static final String TYPE_OPERATION_REMESAS = "3";

    @Inject
    PriceExchangeRepository priceExchangeRepository;

    @Override
    public Uni<Integer> saveAverageRate(
            String currencyBaseCode,
            String currencyQuoteCode,
            BigDecimal averagePrice,
            LocalDate exchangeDate) {

        // Este método ya no se usa - se usa upsertRate con IDs
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

        // Panache.withTransaction() crea una nueva sesión+transacción cada vez que el Uni es suscrito,
        // lo cual funciona correctamente en cadenas secuenciales (a diferencia de @WithTransaction)
        return Panache.withTransaction(() ->
            // Paso 1: Desactivar cualquier registro activo existente para esta fecha y par de monedas
            priceExchangeRepository.deactivateRatesByDate(
                    exchangeDate, currencyBaseId, currencyQuoteId, TYPE_OPERATION_REMESAS
            ).onItem().transformToUni(deactivatedCount -> {
                log.info("=== [DEACTIVATE] Thread: {} | Deactivated count: {} | base:{} -> quote:{} | date: {} ===",
                        Thread.currentThread().getName(), deactivatedCount, currencyBaseId, currencyQuoteId, exchangeDate);

                // Paso 2: Crear nuevo registro activo
                PriceExchangeEntity newEntity = new PriceExchangeEntity();
                newEntity.setTypeOperation(TYPE_OPERATION_REMESAS);
                newEntity.setIdCurrencyBase(currencyBaseId);
                newEntity.setIdCurrencyQuote(currencyQuoteId);
                newEntity.setAmountPrice(averagePrice);
                newEntity.setDateExchange(exchangeDate);
                newEntity.setIsActive("1");

                log.info("=== [CREATE] Thread: {} | Creating entity: base:{} -> quote:{} = {} | date: {} ===",
                        Thread.currentThread().getName(), currencyBaseId, currencyQuoteId, averagePrice, exchangeDate);

                return priceExchangeRepository.persistAndFlush(newEntity)
                        .map(saved -> {
                            log.info("=== [SAVED] Thread: {} | Saved ID: {} | base:{} -> quote:{} = {} ===",
                                    Thread.currentThread().getName(), saved.getId(), currencyBaseId, currencyQuoteId, averagePrice);
                            return saved.getId();
                        });
            })
        ).onFailure().invoke(error ->
                log.error("=== [ERROR] Thread: {} | Failed to upsert rate | base:{} -> quote:{} | error: {} ===",
                        Thread.currentThread().getName(), currencyBaseId, currencyQuoteId, error.getMessage(), error)
        );
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
