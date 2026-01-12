package org.walrex.infrastructure.adapter.outbound.cache.qualifier;

import jakarta.inject.Qualifier;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Qualifier para inyección de dependencias del cache de marcas de producto.
 *
 * Permite diferenciar el bean de cache de ProductBrand de otros caches.
 */
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE})
public @interface ProductBrandCache {
}
