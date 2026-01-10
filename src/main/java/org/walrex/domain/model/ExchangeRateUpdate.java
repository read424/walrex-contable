package org.walrex.domain.model;

import java.util.Map;

/**
 * Record para encapsular el resultado de la actualización de tasas de cambio
 */
public record ExchangeRateUpdate(
        Map<String, RouteRates> ratesByPair
) {
}
