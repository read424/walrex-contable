package org.walrex.infrastructure.adapter.outbound.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.walrex.application.dto.response.CustomerResponse;
import org.walrex.application.dto.response.PagedResponse;
import org.walrex.application.port.output.CustomerCachePort;
import org.walrex.infrastructure.adapter.outbound.cache.qualifier.CustomerCache;

/**
 * Implementación concreta de cache Redis para Customer.
 *
 * Extiende la clase genérica RedisCacheAdapter con CustomerResponse
 * e implementa CustomerCachePort para inyección de dependencias.
 */
@ApplicationScoped
@CustomerCache
public class CustomerRedisCacheAdapter extends RedisCacheAdapter<CustomerResponse> implements CustomerCachePort {

    // Constructor sin argumentos requerido por CDI para proxies
    protected CustomerRedisCacheAdapter() {
        super();
    }

    @Inject
    public CustomerRedisCacheAdapter(ReactiveRedisDataSource reactiveRedisDataSource, ObjectMapper objectMapper) {
        super(
                reactiveRedisDataSource,
                objectMapper,
                new TypeReference<PagedResponse<CustomerResponse>>() {
                },
                "Customer");
    }

    @Override
    protected String getInvalidationPattern() {
        return CustomerCacheKeyGenerator.getInvalidationPattern();
    }
}
