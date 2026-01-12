package org.walrex.application.port.output;

import org.walrex.application.dto.response.ProductUomResponse;

/**
 * Puerto de salida para operaciones de caché de ProductUom.
 *
 * Extiende CachePort con el tipo específico ProductUomResponse.
 * Hereda todas las operaciones de caché (get, put, invalidate, etc.).
 */
public interface ProductUomCachePort extends CachePort<ProductUomResponse> {
}
