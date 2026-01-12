package org.walrex.infrastructure.adapter.outbound.cache.qualifier;

import jakarta.inject.Qualifier;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Calificador para inyección de ProductCategoryUomCachePort.
 * Resuelve ambigüedad cuando hay múltiples implementaciones de CachePort.
 */
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER})
public @interface ProductCategoryUomCache {
}
