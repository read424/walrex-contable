package org.walrex.application.port.output;

import org.walrex.application.dto.response.ProductCategoryUomResponse;

/**
 * Puerto de salida para operaciones de caché de ProductCategoryUom.
 *
 * Siguiendo el patrón hexagonal, este puerto define las operaciones
 * de caché sin especificar la implementación (Redis).
 */
public interface ProductCategoryUomCachePort extends CachePort<ProductCategoryUomResponse> {
}
