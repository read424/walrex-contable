package org.walrex.infrastructure.adapter.outbound.persistence.repository;

import io.vertx.sqlclient.Pool;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class CurrencyQueryRepository {

    @Inject
    Pool client;

    private static final String INSERT_SQL = """
            """;

}
