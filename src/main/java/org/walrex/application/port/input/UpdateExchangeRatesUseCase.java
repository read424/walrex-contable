package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.ExchangeRateRouteInfo;
import org.walrex.domain.model.ExchangeRateUpdate;

import java.math.BigDecimal;

public interface UpdateExchangeRatesUseCase {

    /** Actualiza tasas de rutas BINANCE (Binance P2P). */
    Uni<ExchangeRateUpdate> updateExchangeRates();

    /**
     * Persiste una cross rate calculada externamente para una ruta específica.
     * Aplica la misma lógica que el scheduler: Redis + BD si la variación supera el umbral,
     * solo TTL en Redis si la variación es mínima.
     */
    Uni<Void> saveRateForRoute(ExchangeRateRouteInfo route, BigDecimal crossRate);
}
