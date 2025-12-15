package org.walrex.application.port.output;

import io.smallrye.mutiny.Uni;
import org.walrex.application.dto.response.PagedResponse;
import org.walrex.application.dto.response.SystemDocumentTypeResponse;

import java.time.Duration;

/**
 * Puerto de salida para operaciones de caché de tipos de documento.
 */
public interface SystemDocumentTypeCachePort {
    Uni<PagedResponse<SystemDocumentTypeResponse>> get(String key);

    Uni<Void> put(String key, PagedResponse<SystemDocumentTypeResponse> value, Duration ttl);

    Uni<Void> invalidate(String key);

    Uni<Void> invalidateAll();
}
