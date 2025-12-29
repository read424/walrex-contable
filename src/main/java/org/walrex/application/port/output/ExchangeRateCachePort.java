package org.walrex.application.port.output;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.ExchangeRateCache;

import java.time.Duration;
import java.util.Optional;

/**
 * Puerto de salida para operaciones de caché de tasas de cambio.
 *
 * Permite almacenar y recuperar tasas de cambio en Redis para optimizar
 * las escrituras en base de datos cuando las variaciones son mínimas.
 */
public interface ExchangeRateCachePort {

    /**
     * Obtiene una tasa de cambio del caché.
     *
     * @param key Clave única generada por ExchangeRateCache.generateCacheKey()
     * @return Uni con Optional de ExchangeRateCache (vacío si no existe o expiró)
     */
    Uni<Optional<ExchangeRateCache>> get(String key);

    /**
     * Guarda o actualiza una tasa de cambio en el caché con TTL.
     *
     * @param key Clave única generada por ExchangeRateCache.generateCacheKey()
     * @param value Objeto ExchangeRateCache a almacenar
     * @param ttl Time-To-Live (duración antes de expirar)
     * @return Uni<Void> indicando completitud de la operación
     */
    Uni<Void> set(String key, ExchangeRateCache value, Duration ttl);

    /**
     * Elimina una tasa de cambio del caché.
     *
     * @param key Clave única a eliminar
     * @return Uni<Boolean> indicando si la clave existía y fue eliminada
     */
    Uni<Boolean> delete(String key);
}
