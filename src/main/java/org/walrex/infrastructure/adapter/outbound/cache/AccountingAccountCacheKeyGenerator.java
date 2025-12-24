package org.walrex.infrastructure.adapter.outbound.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.walrex.application.dto.query.AccountingAccountFilter;
import org.walrex.application.dto.query.PageRequest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Generador de claves únicas para caché de accounting accounts (cuentas contables).
 *
 * Genera claves basadas en:
 * - AccountingAccountFilter (todos los campos)
 * - Número de página
 * - Tamaño de página
 * - Campo de ordenamiento
 * - Dirección de ordenamiento
 */
public class AccountingAccountCacheKeyGenerator {

    private static final String CACHE_PREFIX = "accounting-account:list:";
    private static final String CACHE_ALL_PREFIX = "accounting-account:all:";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Genera una clave única de caché para una consulta de listado.
     *
     * Formato: accounting-account:list:{hash}
     * donde {hash} es un SHA-256 de todos los parámetros serializados.
     *
     * @param pageRequest Configuración de paginación y ordenamiento
     * @param filter Filtros aplicados (puede ser null)
     * @return Clave única para esta combinación de parámetros
     */
    public static String generateKey(PageRequest pageRequest, AccountingAccountFilter filter) {
        try {
            // Crear un objeto que combine todos los parámetros
            CacheKeyComponents components = new CacheKeyComponents(
                pageRequest.getPage(),
                pageRequest.getSize(),
                pageRequest.getSortBy(),
                pageRequest.getSortDirection().getValue(),
                filter != null ? filter.getSearch() : null,
                filter != null ? filter.getCode() : null,
                filter != null ? filter.getName() : null,
                filter != null ? filter.getType() : null,
                filter != null ? filter.getNormalSide() : null,
                filter != null ? filter.getActive() : null,
                filter != null ? filter.getIncludeDeleted() : "0"
            );

            // Serializar a JSON para tener una representación canónica
            String jsonRepresentation = objectMapper.writeValueAsString(components);

            // Generar hash SHA-256
            String hash = generateSHA256Hash(jsonRepresentation);

            return CACHE_PREFIX + hash;

        } catch (JsonProcessingException e) {
            // Si falla la serialización, usar una clave basada en string simple
            return CACHE_PREFIX + buildFallbackKey(pageRequest, filter);
        }
    }

    /**
     * Genera una clave única de caché para una consulta de listado sin paginación.
     *
     * Formato: accounting-account:all:{hash}
     * donde {hash} es un SHA-256 de todos los parámetros serializados.
     *
     * @param filter Filtros aplicados (puede ser null)
     * @return Clave única para esta combinación de parámetros
     */
    public static String generateKey(AccountingAccountFilter filter) {
        try {
            // Crear un objeto que combine todos los parámetros
            CacheKeyAllComponents components = new CacheKeyAllComponents(
                    filter != null ? filter.getSearch() : null,
                    filter != null ? filter.getCode() : null,
                    filter != null ? filter.getName() : null,
                    filter != null ? filter.getType() : null,
                    filter != null ? filter.getNormalSide() : null,
                    filter != null ? filter.getActive() : null,
                    filter != null ? filter.getIncludeDeleted() : "0"
            );

            // Serializar a JSON para tener una representación canónica
            String jsonRepresentation = objectMapper.writeValueAsString(components);

            // Generar hash SHA-256
            String hash = generateSHA256Hash(jsonRepresentation);

            return CACHE_ALL_PREFIX + hash;

        } catch (JsonProcessingException e) {
            // Si falla la serialización, usar una clave basada en string simple
            return CACHE_ALL_PREFIX + buildFallbackKeyForAll(filter);
        }
    }

    /**
     * Genera clave para invalidar todo el patrón de accounting accounts.
     * Invalida tanto claves paginadas (accounting-account:list:*) como no paginadas (accounting-account:all:*).
     *
     * @return Patrón para invalidar todas las claves de accounting accounts
     */
    public static String getInvalidationPattern() {
        return "accounting-account:*";
    }

    /**
     * Genera hash SHA-256 de un string.
     */
    private static String generateSHA256Hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            // Fallback: usar hashCode de Java
            return String.valueOf(input.hashCode());
        }
    }

    /**
     * Construye clave alternativa si falla la serialización JSON.
     */
    private static String buildFallbackKey(PageRequest pageRequest, AccountingAccountFilter filter) {
        StringBuilder key = new StringBuilder();
        key.append("page:").append(pageRequest.getPage())
           .append(":size:").append(pageRequest.getSize())
           .append(":sort:").append(pageRequest.getSortBy())
           .append(":dir:").append(pageRequest.getSortDirection().getValue());

        if (filter != null) {
            if (filter.getSearch() != null) {
                key.append(":search:").append(filter.getSearch());
            }
            if (filter.getCode() != null) {
                key.append(":code:").append(filter.getCode());
            }
            if (filter.getName() != null) {
                key.append(":name:").append(filter.getName());
            }
            if (filter.getType() != null) {
                key.append(":type:").append(filter.getType());
            }
            if (filter.getNormalSide() != null) {
                key.append(":normalSide:").append(filter.getNormalSide());
            }
            if (filter.getActive() != null) {
                key.append(":active:").append(filter.getActive());
            }
            key.append(":deleted:").append(filter.getIncludeDeleted());
        }

        return String.valueOf(key.toString().hashCode());
    }

    /**
     * Construye clave alternativa si falla la serialización JSON.
     */
    private static String buildFallbackKeyForAll(AccountingAccountFilter filter) {
        StringBuilder key = new StringBuilder();

        if (filter != null) {
            if (filter.getSearch() != null) {
                key.append(":search:").append(filter.getSearch());
            }
            if (filter.getCode() != null) {
                key.append(":code:").append(filter.getCode());
            }
            if (filter.getName() != null) {
                key.append(":name:").append(filter.getName());
            }
            if (filter.getType() != null) {
                key.append(":type:").append(filter.getType());
            }
            if (filter.getNormalSide() != null) {
                key.append(":normalSide:").append(filter.getNormalSide());
            }
            if (filter.getActive() != null) {
                key.append(":active:").append(filter.getActive());
            }
            key.append(":deleted:").append(filter.getIncludeDeleted());
        }

        return String.valueOf(key.toString().hashCode());
    }

    /**
     * Record interno para serialización de componentes de la clave.
     * Asegura orden consistente de campos en JSON.
     */
    private record CacheKeyComponents(
        int page,
        int size,
        String sortBy,
        String sortDirection,
        String search,
        String code,
        String name,
        String type,
        String normalSide,
        String active,
        String includeDeleted
    ) {}

    /**
     * Record interno para serialización de componentes de la clave /all.
     * Similar a CacheKeyComponents pero sin campos de paginación.
     */
    private record CacheKeyAllComponents(
            String search,
            String code,
            String name,
            String type,
            String normalSide,
            String active,
            String includeDeleted
    ) {}
}
