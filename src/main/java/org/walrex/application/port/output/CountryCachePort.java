package org.walrex.application.port.output;

import org.walrex.application.dto.response.CountryResponse;

/**
 * Puerto de salida para operaciones de caché de Country.
 *
 * Siguiendo el patrón hexagonal, este puerto define las operaciones
 * de caché sin especificar la implementación (Redis, Caffeine, etc.).
 */
public interface CountryCachePort extends CachePort<CountryResponse> {
}