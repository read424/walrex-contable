package org.walrex.infrastructure.adapter.outbound.cache.qualifier;

import jakarta.inject.Qualifier;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Calificador CDI para inyectar el adaptador de cache específico de ProductTemplate.
 *
 * Uso:
 * <pre>
 * {@code @Inject}
 * {@code @ProductTemplateCache}
 * ProductTemplateCachePort cachePort;
 * </pre>
 */
@Qualifier
@Retention(RUNTIME)
@Target({METHOD, FIELD, PARAMETER, TYPE})
public @interface ProductTemplateCache {
}
