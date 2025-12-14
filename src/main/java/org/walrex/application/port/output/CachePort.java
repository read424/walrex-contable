package org.walrex.application.port.output;

import io.smallrye.mutiny.Uni;
import org.walrex.application.dto.response.PagedResponse;

import java.time.Duration;

/**
 * Puerto de salida genérico para operaciones de caché.
 *
 * Siguiendo el patrón hexagonal, este puerto define las operaciones
 * de caché sin especificar la implementación (Redis, Caffeine, etc.).
 *
 * @param <T> Tipo de respuesta que se almacenará en caché
 */
public interface CachePort<T> {

    /**
     * Obtiene un resultado paginado desde la caché.
     *
     * @param cacheKey Clave única que identifica la consulta
     * @return Uni con el resultado paginado si existe en caché, null si no existe
     */
    Uni<PagedResponse<T>> get(String cacheKey);

    /**
     * Almacena un resultado paginado en la caché.
     *
     * @param cacheKey Clave única que identifica la consulta
     * @param value Resultado paginado a cachear
     * @param ttl Tiempo de vida del cache
     * @return Uni que se completa cuando se almacena
     */
    Uni<Void> put(String cacheKey, PagedResponse<T> value, Duration ttl);

    /**
     * Invalida una clave específica en la caché.
     *
     * @param cacheKey Clave a invalidar
     * @return Uni que se completa cuando se invalida
     */
    Uni<Void> invalidate(String cacheKey);

    /**
     * Invalida todas las entradas en la caché.
     * Útil cuando se crea, actualiza o elimina una entidad.
     *
     * @return Uni que se completa cuando se invalidan todas las entradas
     */
    Uni<Void> invalidateAll();
}