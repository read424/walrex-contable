package org.walrex.application.port.input;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.walrex.application.dto.query.CustomerFilter;
import org.walrex.application.dto.query.PageRequest;
import org.walrex.application.dto.response.CustomerResponse;
import org.walrex.application.dto.response.PagedResponse;

public interface ListCustomersUseCase {
    /**
     * Lista clientes con paginación y filtros.
     *
     * @param pageRequest Configuración de paginación (page, size, sort)
     * @param filter      Filtros opcionales (search, idTypeDocument,
     *                    numberDocument, etc.)
     * @return Uni con respuesta paginada
     */
    Uni<PagedResponse<CustomerResponse>> listar(PageRequest pageRequest, CustomerFilter filter);

    /**
     * Obtiene todos los clientes activos como un stream reactivo.
     *
     * Útil para:
     * - Exportaciones
     * - Procesamiento en streaming
     * - Server-Sent Events (SSE)
     *
     * @return Multi que emite cada cliente individualmente
     */
    Multi<CustomerResponse> streamAll();

    /**
     * Obtiene todos los clientes activos como un stream con filtros.
     *
     * @param filter Filtros a aplicar
     * @return Multi que emite cada cliente que cumple los filtros
     */
    Multi<CustomerResponse> streamWithFilter(CustomerFilter filter);
}
