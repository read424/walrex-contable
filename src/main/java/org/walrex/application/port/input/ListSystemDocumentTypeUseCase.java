package org.walrex.application.port.input;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.walrex.application.dto.query.PageRequest;
import org.walrex.application.dto.query.SystemDocumentTypeFilter;
import org.walrex.application.dto.response.PagedResponse;
import org.walrex.application.dto.response.SystemDocumentTypeResponse;

/**
 * Puerto de entrada para listar tipos de documento del sistema.
 */
public interface ListSystemDocumentTypeUseCase {
    Uni<PagedResponse<SystemDocumentTypeResponse>> listar(PageRequest pageRequest, SystemDocumentTypeFilter filter);

    Multi<SystemDocumentTypeResponse> streamAll();

    Multi<SystemDocumentTypeResponse> streamWithFilter(SystemDocumentTypeFilter filter);
}
