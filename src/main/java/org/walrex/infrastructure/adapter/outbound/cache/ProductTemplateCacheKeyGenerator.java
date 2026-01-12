package org.walrex.infrastructure.adapter.outbound.cache;

import org.walrex.application.dto.query.PageRequest;
import org.walrex.application.dto.query.ProductTemplateFilter;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Generador de claves de cache para ProductTemplate.
 *
 * Genera claves únicas basadas en los parámetros de búsqueda usando SHA-256.
 * Esto asegura que diferentes combinaciones de filtros tengan claves únicas
 * pero predecibles (la misma combinación siempre genera la misma clave).
 */
public class ProductTemplateCacheKeyGenerator {

    private static final String PREFIX = "product-template";

    /**
     * Genera una clave de cache para búsquedas paginadas.
     *
     * @param pageRequest Configuración de paginación
     * @param filter Filtros de búsqueda
     * @return Clave única de cache
     */
    public static String generateKey(PageRequest pageRequest, ProductTemplateFilter filter) {
        StringBuilder keyBuilder = new StringBuilder(PREFIX)
                .append(":page:").append(pageRequest.getPage())
                .append(":size:").append(pageRequest.getSize())
                .append(":sort:").append(pageRequest.getSortBy())
                .append(":dir:").append(pageRequest.getSortDirection());

        appendFilterToKey(keyBuilder, filter);

        return hashKey(keyBuilder.toString());
    }

    /**
     * Genera una clave de cache para búsquedas sin paginación (/all endpoint).
     *
     * @param filter Filtros de búsqueda
     * @return Clave única de cache
     */
    public static String generateKey(ProductTemplateFilter filter) {
        StringBuilder keyBuilder = new StringBuilder(PREFIX).append(":all");

        appendFilterToKey(keyBuilder, filter);

        return hashKey(keyBuilder.toString());
    }

    /**
     * Agrega los filtros a la clave de cache.
     */
    private static void appendFilterToKey(StringBuilder keyBuilder, ProductTemplateFilter filter) {
        if (filter == null) {
            return;
        }

        if (filter.getSearch() != null) {
            keyBuilder.append(":search:").append(filter.getSearch());
        }
        if (filter.getName() != null) {
            keyBuilder.append(":name:").append(filter.getName());
        }
        if (filter.getInternalReference() != null) {
            keyBuilder.append(":ref:").append(filter.getInternalReference());
        }
        if (filter.getType() != null) {
            keyBuilder.append(":type:").append(filter.getType().getValue());
        }
        if (filter.getCategoryId() != null) {
            keyBuilder.append(":cat:").append(filter.getCategoryId());
        }
        if (filter.getBrandId() != null) {
            keyBuilder.append(":brand:").append(filter.getBrandId());
        }
        if (filter.getStatus() != null) {
            keyBuilder.append(":status:").append(filter.getStatus());
        }
        if (filter.getCanBeSold() != null) {
            keyBuilder.append(":sold:").append(filter.getCanBeSold());
        }
        if (filter.getCanBePurchased() != null) {
            keyBuilder.append(":purch:").append(filter.getCanBePurchased());
        }
        if (filter.getHasVariants() != null) {
            keyBuilder.append(":var:").append(filter.getHasVariants());
        }
        if (filter.getIncludeDeleted() != null) {
            keyBuilder.append(":del:").append(filter.getIncludeDeleted());
        }
    }

    /**
     * Genera clave para invalidar todo el patrón de product template.
     * Invalida tanto claves paginadas como no paginadas.
     *
     * @return Patrón para invalidar todas las claves de product template
     */
    public static String getInvalidationPattern() {
        return PREFIX + ":*";
    }

    /**
     * Genera un hash SHA-256 de la clave para mantenerla corta y única.
     */
    private static String hashKey(String key) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(key.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return PREFIX + ":" + hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            // Fallback to the original key if SHA-256 is not available
            return key;
        }
    }
}
