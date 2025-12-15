package org.walrex.infrastructure.adapter.outbound.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.walrex.application.dto.response.CountryResponse;
import org.walrex.application.dto.response.PagedResponse;
import org.walrex.application.port.output.CountryCachePort;
import org.walrex.infrastructure.adapter.outbound.cache.qualifier.CountryCache;

/**
 * Implementación concreta de cache Redis para Country.
 *
 * Extiende la clase genérica RedisCacheAdapter con CountryResponse
 * e implementa CountryCachePort para inyección de dependencias.
 */
@ApplicationScoped
@CountryCache
public class CountryRedisCacheAdapter extends RedisCacheAdapter<CountryResponse> implements CountryCachePort {

    // Constructor sin argumentos requerido por CDI para proxies
    protected CountryRedisCacheAdapter() {
        super();
    }

    @Inject
    public CountryRedisCacheAdapter(ReactiveRedisDataSource reactiveRedisDataSource, ObjectMapper objectMapper) {
        super(
            reactiveRedisDataSource,
            objectMapper,
            new TypeReference<PagedResponse<CountryResponse>>() {},
            "Country"
        );
    }

    @Override
    protected String getInvalidationPattern() {
        return CountryCacheKeyGenerator.getInvalidationPattern();
    }
}