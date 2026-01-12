package org.walrex.infrastructure.adapter.outbound.cache.qualifier;

import jakarta.inject.Qualifier;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Qualifier para identificar la implementación de cache específica de ProductUom.
 *
 * Se usa con @Inject para inyectar la implementación correcta del cache:
 *
 * <pre>
 * {@code
 * @Inject
 * @ProductUomCache
 * ProductUomCachePort cachePort;
 * }
 * </pre>
 */
@Qualifier
@Retention(RUNTIME)
@Target({METHOD, FIELD, PARAMETER, TYPE})
public @interface ProductUomCache {
}
