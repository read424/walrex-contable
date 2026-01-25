package org.walrex.infrastructure.adapter.outbound.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.walrex.application.dto.response.OcupacionResponse;
import org.walrex.application.dto.response.PagedResponse;
import org.walrex.infrastructure.adapter.outbound.cache.qualifier.OcupacionCache;

@ApplicationScoped
@OcupacionCache
public class OcupacionRedisCacheAdapter extends RedisCacheAdapter<OcupacionResponse> {

    private final OcupacionCacheKeyGenerator keyGenerator;

    // Constructor sin argumentos para CDI
    public OcupacionRedisCacheAdapter() {
        super();
        this.keyGenerator = null; // Será inyectado por el otro constructor
    }

    @Inject
    public OcupacionRedisCacheAdapter(ReactiveRedisDataSource reactiveRedisDataSource,
                                     ObjectMapper objectMapper,
                                     OcupacionCacheKeyGenerator keyGenerator) {
        super(reactiveRedisDataSource,
              objectMapper,
              new TypeReference<PagedResponse<OcupacionResponse>>() {},
              "ocupacion");
        this.keyGenerator = keyGenerator;
    }

    @Override
    protected String getInvalidationPattern() {
        return keyGenerator.getInvalidationPattern();
    }
}
