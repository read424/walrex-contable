package org.walrex.infrastructure.adapter.outbound.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.walrex.application.dto.response.PagedResponse;
import org.walrex.application.dto.response.BeneficiaryAccountResponse;
import org.walrex.application.port.output.BeneficiaryAccountCachePort;
import org.walrex.infrastructure.adapter.outbound.cache.qualifier.BeneficiaryAccountCache;

@ApplicationScoped
@BeneficiaryAccountCache
public class BeneficiaryAccountRedisCacheAdapter extends RedisCacheAdapter<BeneficiaryAccountResponse> implements BeneficiaryAccountCachePort {

    protected BeneficiaryAccountRedisCacheAdapter() {
        super();
    }

    @Inject
    public BeneficiaryAccountRedisCacheAdapter(ReactiveRedisDataSource reactiveRedisDataSource, ObjectMapper objectMapper) {
        super(
            reactiveRedisDataSource,
            objectMapper,
            new TypeReference<PagedResponse<BeneficiaryAccountResponse>>() {},
            "BeneficiaryAccount"
        );
    }

    @Override
    protected String getInvalidationPattern() {
        return BeneficiaryAccountCacheKeyGenerator.getInvalidationPattern();
    }
}
