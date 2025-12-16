package org.walrex.application.port.output;

import org.walrex.application.dto.response.CustomerResponse;

/**
 * Puerto de salida para operaciones de caché de Customer.
 *
 * Siguiendo el patrón hexagonal, este puerto define las operaciones
 * de caché sin especificar la implementación (Redis, Caffeine, etc.).
 */
public interface CustomerCachePort extends CachePort<CustomerResponse> {
}
