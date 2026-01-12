package org.walrex.application.port.output;

import org.walrex.application.dto.response.ProductAttributeResponse;

/**
 * Puerto de salida para operaciones de caché de ProductAttribute.
 *
 * Siguiendo el patrón hexagonal, este puerto define las operaciones
 * de caché sin especificar la implementación (Redis).
 */
public interface ProductAttributeCachePort extends CachePort<ProductAttributeResponse> {
}
