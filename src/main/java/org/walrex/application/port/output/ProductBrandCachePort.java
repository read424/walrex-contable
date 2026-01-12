package org.walrex.application.port.output;

import org.walrex.application.dto.response.ProductBrandResponse;

/**
 * Puerto de salida para operaciones de cache de marcas de producto.
 *
 * Extiende CachePort con ProductBrandResponse como tipo genérico.
 * Será implementado por el adaptador de cache Redis.
 */
public interface ProductBrandCachePort extends CachePort<ProductBrandResponse> {
}
