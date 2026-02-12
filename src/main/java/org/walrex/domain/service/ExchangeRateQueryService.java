package org.walrex.domain.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.walrex.application.dto.response.ExchangeRateQueryResponse;
import org.walrex.application.port.input.GetExchangeRateUseCase;
import org.walrex.application.port.output.ExchangeRateCachePort;
import org.walrex.application.port.output.PriceExchangeOutputPort;
import org.walrex.domain.model.ExchangeRateCache;

import java.time.LocalDate;

@Slf4j
@ApplicationScoped
public class ExchangeRateQueryService implements GetExchangeRateUseCase {

    @Inject
    ExchangeRateCachePort cachePort;

    @Inject
    PriceExchangeOutputPort priceExchangePort;

    @Override
    public Uni<ExchangeRateQueryResponse> getRate(
            String fromCountry, String fromCurrency,
            String toCountry, String toCurrency) {

        LocalDate today = LocalDate.now();
        String cacheKey = ExchangeRateCache.generateCacheKey(
                fromCountry, fromCurrency, toCountry, toCurrency, today);

        log.info("=== [GET RATE] Querying rate {}:{} -> {}:{} | cacheKey: {} ===",
                fromCountry, fromCurrency, toCountry, toCurrency, cacheKey);

        return cachePort.get(cacheKey)
                .flatMap(cachedOptional -> {
                    if (cachedOptional.isPresent()) {
                        ExchangeRateCache cached = cachedOptional.get();
                        log.info("=== [CACHE HIT] Rate found in Redis: {} ===", cached.getRate());
                        return Uni.createFrom().item(new ExchangeRateQueryResponse(
                                fromCountry, fromCurrency, toCountry, toCurrency,
                                cached.getRate(), today));
                    }

                    log.info("=== [CACHE MISS] Querying database for {}:{} -> {}:{} ===",
                            fromCountry, fromCurrency, toCountry, toCurrency);

                    return priceExchangePort.findActiveRateByCountriesAndCurrencies(
                            fromCountry, fromCurrency, toCountry, toCurrency
                    ).map(dbOptional -> {
                        if (dbOptional.isPresent()) {
                            log.info("=== [DB HIT] Rate found in DB: {} ===", dbOptional.get());
                            return new ExchangeRateQueryResponse(
                                    fromCountry, fromCurrency, toCountry, toCurrency,
                                    dbOptional.get(), today);
                        }

                        log.warn("=== [NOT FOUND] No rate for {}:{} -> {}:{} ===",
                                fromCountry, fromCurrency, toCountry, toCurrency);
                        throw new IllegalArgumentException(String.format(
                                "No exchange rate found for %s:%s -> %s:%s",
                                fromCountry, fromCurrency, toCountry, toCurrency));
                    });
                });
    }
}
