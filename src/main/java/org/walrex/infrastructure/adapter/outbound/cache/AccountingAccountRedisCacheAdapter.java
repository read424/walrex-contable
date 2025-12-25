package org.walrex.infrastructure.adapter.outbound.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.walrex.application.dto.response.AccountingAccountResponse;
import org.walrex.application.dto.response.PagedResponse;
import org.walrex.application.port.output.AccountingAccountCachePort;
import org.walrex.infrastructure.adapter.outbound.cache.qualifier.AccountingAccountCache;

/**
 * Implementación concreta de cache Redis para AccountingAccount.
 *
 * Extiende la clase genérica RedisCacheAdapter con AccountingAccountResponse
 * e implementa AccountingAccountCachePort para inyección de dependencias.
 */
@ApplicationScoped
@AccountingAccountCache
public class AccountingAccountRedisCacheAdapter extends RedisCacheAdapter<AccountingAccountResponse> implements AccountingAccountCachePort {

    // Constructor sin argumentos requerido por CDI para proxies
    protected AccountingAccountRedisCacheAdapter() {
        super();
    }

    @Inject
    public AccountingAccountRedisCacheAdapter(ReactiveRedisDataSource reactiveRedisDataSource, ObjectMapper objectMapper) {
        super(
            reactiveRedisDataSource,
            objectMapper,
            new TypeReference<PagedResponse<AccountingAccountResponse>>() {},
            "AccountingAccount"
        );
    }

    @Override
    protected String getInvalidationPattern() {
        return AccountingAccountCacheKeyGenerator.getInvalidationPattern();
    }
}
