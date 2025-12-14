package org.walrex.infrastructure.adapter.outbound.cache;

import org.walrex.application.dto.query.PageRequest;
import org.walrex.application.dto.query.SystemDocumentTypeFilter;

import java.util.StringJoiner;

/**
 * Generador de claves de cache para SystemDocumentType.
 *
 * Genera claves únicas basadas en los parámetros de búsqueda para
 * evitar colisiones en Redis.
 */
public class SystemDocumentTypeCacheKeyGenerator {

    private static final String PREFIX = "system_document_types";

    /**
     * Genera una clave única de cache basada en los parámetros de paginación y
     * filtros.
     */
    public static String generateKey(PageRequest pageRequest, SystemDocumentTypeFilter filter) {
        StringBuilder key = new StringBuilder(PREFIX);
        key.append(":page:").append(pageRequest.getPage());
        key.append(":size:").append(pageRequest.getSize());
        key.append(":sort:").append(pageRequest.getSortBy());
        key.append(":dir:").append(pageRequest.getSortDirection());

        if (filter != null) {
            if (filter.getSearch() != null && !filter.getSearch().isBlank()) {
                key.append(":search:").append(filter.getSearch());
            }
            if (filter.getCode() != null && !filter.getCode().isBlank()) {
                key.append(":code:").append(filter.getCode());
            }
            if (filter.getIsRequired() != null) {
                key.append(":required:").append(filter.getIsRequired());
            }
            if (filter.getForPerson() != null) {
                key.append(":person:").append(filter.getForPerson());
            }
            if (filter.getForCompany() != null) {
                key.append(":company:").append(filter.getForCompany());
            }
            if (filter.getActive() != null) {
                key.append(":active:").append(filter.getActive());
            }
            if (filter.getIncludeDeleted() != null) {
                key.append(":deleted:").append(filter.getIncludeDeleted());
            }
        }

        return key.toString();
    }

    /**
     * Retorna el patrón para invalidar todas las claves de cache de
     * SystemDocumentType.
     */
    public static String getInvalidationPattern() {
        return PREFIX + ":*";
    }
}