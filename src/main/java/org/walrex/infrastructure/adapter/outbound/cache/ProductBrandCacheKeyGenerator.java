package org.walrex.infrastructure.adapter.outbound.cache;

/**
 * Generador de claves de cache para marcas de producto.
 *
 * Genera claves únicas y consistentes para cachear resultados de consultas.
 */
public class ProductBrandCacheKeyGenerator {

    private static final String PREFIX = "product-brand";
    private static final String ALL_ACTIVE = PREFIX + ":all:active";

    /**
     * Genera una clave de cache para listar todas las marcas activas.
     *
     * @return Clave de cache
     */
    public static String generateKeyForActive() {
        return ALL_ACTIVE;
    }

    /**
     * Patrón de invalidación para eliminar todas las claves relacionadas con marcas.
     *
     * @return Patrón glob para invalidación
     */
    public static String getInvalidationPattern() {
        return PREFIX + ":*";
    }
}
