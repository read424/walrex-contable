package org.walrex.infrastructure.adapter.outbound.persistence;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.walrex.application.port.output.PriceExchangeOutputPort;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.PriceExchangeEntity;
import org.walrex.infrastructure.adapter.outbound.persistence.repository.PriceExchangeRepository;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Adaptador de persistencia para tasas de cambio
 */
@Slf4j
@ApplicationScoped
public class PriceExchangePersistenceAdapter implements PriceExchangeOutputPort {

    private static final String TYPE_OPERATION_REMESAS = "3";

    @Inject
    PriceExchangeRepository priceExchangeRepository;

    @Override
    @WithTransaction
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
    @WithTransaction
    public Uni<Integer> upsertRate(
            Integer currencyBaseId,
            Integer currencyQuoteId,
            BigDecimal averagePrice,
            LocalDate exchangeDate) {

        log.info("Upserting rate for currency base ID:{} -> quote ID:{} on {} with price {}",
                currencyBaseId, currencyQuoteId, exchangeDate, averagePrice);

        // Paso 1: Desactivar cualquier registro activo existente para esta fecha y par de monedas
        return priceExchangeRepository.deactivateRatesByDate(
                exchangeDate, currencyBaseId, currencyQuoteId, TYPE_OPERATION_REMESAS
        ).onItem().transformToUni(deactivatedCount -> {
            if (deactivatedCount > 0) {
                log.info("Deactivated {} existing active rate(s) for base:{} quote:{} on {}",
                        deactivatedCount, currencyBaseId, currencyQuoteId, exchangeDate);
            }

            // Paso 2: Crear nuevo registro activo
            PriceExchangeEntity newEntity = new PriceExchangeEntity();
            newEntity.setTypeOperation(TYPE_OPERATION_REMESAS);
            newEntity.setIdCurrencyBase(currencyBaseId);
            newEntity.setIdCurrencyQuote(currencyQuoteId);
            newEntity.setAmountPrice(averagePrice);
            newEntity.setDateExchange(exchangeDate);
            newEntity.setIsActive("1");

            log.info("Creating new active rate: base ID:{} -> quote ID:{} = {} (date: {})",
                    currencyBaseId, currencyQuoteId, averagePrice, exchangeDate);

            return priceExchangeRepository.persistAndFlush(newEntity)
                    .map(saved -> {
                        log.info("Successfully saved new rate with ID: {}", saved.getId());
                        return saved.getId();
                    });
        });
    }
}
