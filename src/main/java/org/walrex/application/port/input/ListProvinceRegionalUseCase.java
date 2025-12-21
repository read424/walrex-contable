package org.walrex.application.port.input;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.walrex.application.dto.query.DepartamentFilter;
import org.walrex.application.dto.query.PageRequest;
import org.walrex.application.dto.query.ProvinceFilter;
import org.walrex.application.dto.response.DepartamentResponse;
import org.walrex.application.dto.response.PagedResponse;
import org.walrex.application.dto.response.ProvinceResponse;

public interface ListProvinceRegionalUseCase {
    /**
     * Lista departamentos regionales con paginación y filtros.
     *
     * @param pageRequest Configuración de paginación (page, size, sort)
     * @param filter Filtros opcionales (search, codigo, etc.)
     * @return Uni con respuesta paginada
     */
    Uni<PagedResponse<ProvinceResponse>> listar(PageRequest pageRequest, ProvinceFilter filter);

    /**
     * Obtiene todos las provincias regionales activas como un stream reactivo.
     *
     * Útil para:
     * - Exportaciones
     * - Procesamiento en streaming
     * - Server-Sent Events (SSE)
     *
     * @return Multi que emite cada provincia individualmente
     */
    Multi<ProvinceResponse> streamAll();

    /**
     * Obtiene todos las provincias regionales activos como un stream con filtros.
     *
     * @param filter Filtros a aplicar
     * @return Multi que emite cada provincia que cumple los filtros
     */
    Multi<ProvinceResponse> streamWithFilter(ProvinceFilter filter);
}
