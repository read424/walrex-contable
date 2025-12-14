package org.walrex.application.port.output;

import io.smallrye.mutiny.Uni;
import org.walrex.application.dto.response.PagedResponse;
import org.walrex.application.dto.response.SystemDocumentTypeResponse;

import java.time.Duration;

/**
 * Output port for System Document Type caching operations.
 */
public interface SystemDocumentTypeCachePort {
    Uni<PagedResponse<SystemDocumentTypeResponse>> get(String key);

    Uni<Void> put(String key, PagedResponse<SystemDocumentTypeResponse> value, Duration ttl);

    Uni<Void> invalidate(String key);

    Uni<Void> invalidateAll();
}
