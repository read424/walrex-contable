package org.walrex.infrastructure.adapter.outbound.cache;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.commons.lang3.StringUtils;

@ApplicationScoped
public class OcupacionCacheKeyGenerator {

    private static final String OCUPACION_PREFIX = "ocupacion:";
    private static final String ALL_PAGINATED_PREFIX = OCUPACION_PREFIX + "all:paginated:";
    private static final String ALL_PREFIX = OCUPACION_PREFIX + "all";

    public String generateKeyForFindAllPaginated(Integer page, Integer size, String nombreFilter) {
        String filterPart = StringUtils.isBlank(nombreFilter) ? "all" : nombreFilter.toLowerCase();
        return ALL_PAGINATED_PREFIX + "page:" + page + ":size:" + size + ":filter:" + filterPart;
    }

    public String generateKeyForFindAllNoPaginated(String nombreFilter) {
        String filterPart = StringUtils.isBlank(nombreFilter) ? "all" : nombreFilter.toLowerCase();
        return ALL_PREFIX + ":filter:" + filterPart;
    }

    public String getInvalidationPattern() {
        return OCUPACION_PREFIX + "*";
    }

}
