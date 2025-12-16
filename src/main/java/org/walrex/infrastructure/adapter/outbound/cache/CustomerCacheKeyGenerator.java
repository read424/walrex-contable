package org.walrex.infrastructure.adapter.outbound.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.walrex.application.dto.query.CustomerFilter;
import org.walrex.application.dto.query.PageRequest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Generador de claves únicas para caché de customers.
 *
 * Genera claves basadas en:
 * - CustomerFilter (todos los campos)
 * - Número de página
 * - Tamaño de página
 * - Campo de ordenamiento
 * - Dirección de ordenamiento
 */
public class CustomerCacheKeyGenerator {

    private static final String CACHE_PREFIX = "customer:list:";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Genera una clave única de caché para una consulta de listado.
     *
     * Formato: customer:list:{hash}
     * donde {hash} es un SHA-256 de todos los parámetros serializados.
     *
     * @param pageRequest Configuración de paginación y ordenamiento
     * @param filter      Filtros aplicados (puede ser null)
     * @return Clave única para esta combinación de parámetros
     */
    public static String generateKey(PageRequest pageRequest, CustomerFilter filter) {
        try {
            // Crear un objeto que combine todos los parámetros
            CustomerCacheKeyGenerator.CacheKeyComponents components = new CustomerCacheKeyGenerator.CacheKeyComponents(
                    pageRequest.getPage(),
                    pageRequest.getSize(),
                    pageRequest.getSortBy(),
                    pageRequest.getSortDirection().getValue(),
                    filter != null ? filter.getSearch() : null,
                    filter != null ? filter.getIdTypeDocument() : null,
                    filter != null ? filter.getNumberDocument() : null,
                    filter != null ? filter.getEmail() : null,
                    filter != null ? filter.getGender() : null,
                    filter != null ? filter.getIdCountryResidence() : null,
                    filter != null ? filter.getIsPEP() : null,
                    filter != null ? filter.getIncludeDeleted() : "1");

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
     * Genera clave para invalidar todo el patrón de customers.
     *
     * @return Patrón para invalidar todas las claves de customer
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
    private static String buildFallbackKey(PageRequest pageRequest, CustomerFilter filter) {
        StringBuilder key = new StringBuilder();
        key.append("page:").append(pageRequest.getPage())
                .append(":size:").append(pageRequest.getSize())
                .append(":sort:").append(pageRequest.getSortBy())
                .append(":dir:").append(pageRequest.getSortDirection().getValue());

        if (filter != null) {
            if (filter.getSearch() != null)
                key.append(":s:").append(filter.getSearch());
            if (filter.getIdTypeDocument() != null)
                key.append(":t:").append(filter.getIdTypeDocument());
            if (filter.getNumberDocument() != null)
                key.append(":n:").append(filter.getNumberDocument());
            if (filter.getEmail() != null)
                key.append(":e:").append(filter.getEmail());
            if (filter.getGender() != null)
                key.append(":g:").append(filter.getGender());
            if (filter.getIdCountryResidence() != null)
                key.append(":r:").append(filter.getIdCountryResidence());
            if (filter.getIsPEP() != null)
                key.append(":p:").append(filter.getIsPEP());
            key.append(":d:").append(filter.getIncludeDeleted());
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
            Integer idTypeDocument,
            String numberDocument,
            String email,
            String gender,
            Integer idCountryResidence,
            String isPEP,
            String includeDeleted) {
    }
}
