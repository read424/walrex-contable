package org.walrex.infrastructure.adapter.outbound.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.walrex.application.dto.query.CurrencyFilter;
import org.walrex.application.dto.query.PageRequest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Generador de claves únicas para caché de currencies.
 *
 * Genera claves basadas en:
 * - CurrencyFilter (todos los campos)
 * - Número de página
 * - Tamaño de página
 * - Campo de ordenamiento
 * - Dirección de ordenamiento
 */
public class CurrencyCacheKeyGenerator {

    private static final String CACHE_PREFIX = "currency:list:";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Genera una clave única de caché para una consulta de listado.
     *
     * Formato: currency:list:{hash}
     * donde {hash} es un SHA-256 de todos los parámetros serializados.
     *
     * @param pageRequest Configuración de paginación y ordenamiento
     * @param filter Filtros aplicados (puede ser null)
     * @return Clave única para esta combinación de parámetros
     */
    public static String generateKey(PageRequest pageRequest, CurrencyFilter filter) {
        try {
            // Crear un objeto que combine todos los parámetros
            CacheKeyComponents components = new CacheKeyComponents(
                pageRequest.getPage(),
                pageRequest.getSize(),
                pageRequest.getSortBy(),
                pageRequest.getSortDirection().getValue(),
                filter != null ? filter.getSearch() : null,
                filter != null ? filter.getAlphabeticCode() : null,
                filter != null ? filter.getNumericCode() : null,
                filter != null ? filter.getStatus() : null,
                filter != null ? filter.getIncludeDeleted() : "1"
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
     * Genera clave para invalidar todo el patrón de currencies.
     *
     * @return Patrón para invalidar todas las claves de currency
     */
    public static String getInvalidationPattern() {
        return CACHE_PREFIX + "*";
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
    private static String buildFallbackKey(PageRequest pageRequest, CurrencyFilter filter) {
        StringBuilder key = new StringBuilder();
        key.append("page:").append(pageRequest.getPage())
           .append(":size:").append(pageRequest.getSize())
           .append(":sort:").append(pageRequest.getSortBy())
           .append(":dir:").append(pageRequest.getSortDirection().getValue());

        if (filter != null) {
            if (filter.getSearch() != null) {
                key.append(":search:").append(filter.getSearch());
            }
            if (filter.getAlphabeticCode() != null) {
                key.append(":alpha:").append(filter.getAlphabeticCode());
            }
            if (filter.getNumericCode() != null) {
                key.append(":num:").append(filter.getNumericCode());
            }
            if (filter.getStatus() != null) {
                key.append(":status:").append(filter.getStatus());
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
        String alphabeticCode,
        String numericCode,
        String status,
        String includeDeleted
    ) {}
}