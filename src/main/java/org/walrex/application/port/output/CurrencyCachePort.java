package org.walrex.application.port.output;

import org.walrex.application.dto.response.CurrencyResponse;

/**
 * Puerto de salida para operaciones de caché de Currency.
 *
 * Siguiendo el patrón hexagonal, este puerto define las operaciones
 * de caché sin especificar la implementación (Redis, Caffeine, etc.).
 */
public interface CurrencyCachePort extends CachePort<CurrencyResponse> {
}