package org.walrex.application.port.output;

import org.walrex.application.dto.response.ProductAttributeValueResponse;

/**
 * Puerto de salida (Output Port) para operaciones de caché de valores de atributos de producto.
 *
 * Extiende CachePort con ProductAttributeValueResponse como tipo genérico.
 * Esto proporciona operaciones estándar de caché:
 * - get: Obtener valor del caché
 * - put: Guardar valor en caché con TTL
 * - invalidate: Invalidar entrada específica del caché
 * - invalidateAll: Invalidar todo el caché de valores de atributos
 *
 * Patrón Hexagonal:
 * Este puerto es DEFINIDO en la capa de aplicación pero IMPLEMENTADO
 * en la capa de infraestructura (por el Adapter de Redis).
 */
public interface ProductAttributeValueCachePort extends CachePort<ProductAttributeValueResponse> {
    // Hereda todos los métodos de CachePort<ProductAttributeValueResponse>
    // No requiere métodos adicionales
}
