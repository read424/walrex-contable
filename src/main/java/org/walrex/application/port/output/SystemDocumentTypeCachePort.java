package org.walrex.application.port.output;

import org.walrex.application.dto.response.SystemDocumentTypeResponse;

/**
 * Puerto de salida para operaciones de caché de SystemDocumentType.
 *
 * Siguiendo el patrón hexagonal, este puerto define las operaciones
 * de caché sin especificar la implementación (Redis, Caffeine, etc.).
 */
public interface SystemDocumentTypeCachePort extends CachePort<SystemDocumentTypeResponse> {
}
