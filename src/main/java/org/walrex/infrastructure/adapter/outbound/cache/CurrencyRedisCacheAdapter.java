package org.walrex.infrastructure.adapter.outbound.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.NoArgsConstructor;
import org.walrex.application.dto.response.CurrencyResponse;
import org.walrex.application.dto.response.PagedResponse;
import org.walrex.application.port.output.CurrencyCachePort;
import org.walrex.infrastructure.adapter.outbound.cache.qualifier.CurrencyCache;

/**
 * Implementación concreta de cache Redis para Currency.
 *
 * Extiende la clase genérica RedisCacheAdapter con CurrencyResponse
 * e implementa CurrencyCachePort para inyección de dependencias.
 */
@ApplicationScoped
@CurrencyCache
public class CurrencyRedisCacheAdapter extends RedisCacheAdapter<CurrencyResponse> implements CurrencyCachePort {

    // Constructor sin argumentos requerido por CDI para proxies
    protected CurrencyRedisCacheAdapter() {
        super();
    }

    @Inject
    public CurrencyRedisCacheAdapter(ReactiveRedisDataSource reactiveRedisDataSource, ObjectMapper objectMapper) {
        super(
            reactiveRedisDataSource,
            objectMapper,
            new TypeReference<PagedResponse<CurrencyResponse>>() {},
            "Currency"
        );
    }

    @Override
    protected String getInvalidationPattern() {
        return CurrencyCacheKeyGenerator.getInvalidationPattern();
    }
}