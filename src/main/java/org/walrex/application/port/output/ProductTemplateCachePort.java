package org.walrex.application.port.output;

import org.walrex.application.dto.response.ProductTemplateResponse;

/**
 * Puerto de salida para operaciones de cache de plantillas de producto.
 *
 * Extiende CachePort con ProductTemplateResponse como tipo genérico.
 * Será implementado por el adaptador de cache Redis.
 */
public interface ProductTemplateCachePort extends CachePort<ProductTemplateResponse> {
}
