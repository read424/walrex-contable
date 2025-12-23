package org.walrex.application.port.input;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.walrex.application.dto.query.DepartamentFilter;
import org.walrex.application.dto.query.PageRequest;
import org.walrex.application.dto.response.DepartamentResponse;
import org.walrex.application.dto.response.PagedResponse;

/**
 * Use case contract for listing and streaming regional departments.
 *
 * Provides operations for retrieving departments with pagination,
 * filtering, and reactive streaming capabilities.
 *
 * @version 1.0
 * @since 1.0
 */
public interface ListDepartmentRegionalUseCase {
    /**
     * Lista departamentos regionales con paginación y filtros.
     *
     * @param pageRequest Configuración de paginación (page, size, sort)
     * @param filter Filtros opcionales (search, codigo, etc.)
     * @return Uni con respuesta paginada
     */
    Uni<PagedResponse<DepartamentResponse>> listar(PageRequest pageRequest, DepartamentFilter filter);

    /**
     * Obtiene todos los departamentos regionales activas como un stream reactivo.
     *
     * Útil para:
     * - Exportaciones
     * - Procesamiento en streaming
     * - Server-Sent Events (SSE)
     *
     * @return Multi que emite cada moneda individualmente
     */
    Multi<DepartamentResponse> streamAll();

    /**
     * Obtiene todos los departamentos regionales activos como un stream con filtros.
     *
     * @param filter Filtros a aplicar
     * @return Multi que emite cada moneda que cumple los filtros
     */
    Multi<DepartamentResponse> streamWithFilter(DepartamentFilter filter);
}
