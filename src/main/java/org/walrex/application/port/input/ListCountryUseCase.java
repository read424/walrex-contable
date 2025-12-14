package org.walrex.application.port.input;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.walrex.application.dto.query.CountryFilter;
import org.walrex.application.dto.query.PageRequest;
import org.walrex.application.dto.response.CountryResponse;
import org.walrex.application.dto.response.PagedResponse;

public interface ListCountryUseCase {
    /**
     * Lista paises con paginación y filtros.
     *
     * @param pageRequest Configuración de paginación (page, size, sort)
     * @param filter Filtros opcionales (search, alphabeticCode, etc.)
     * @return Uni con respuesta paginada
     */
    Uni<PagedResponse<CountryResponse>> listar(PageRequest pageRequest, CountryFilter filter);

    /**
     * Obtiene todos los paises activas como un stream reactivo.
     *
     * Útil para:
     * - Exportaciones
     * - Procesamiento en streaming
     * - Server-Sent Events (SSE)
     *
     * @return Multi que emite cada moneda individualmente
     */
    Multi<CountryResponse> streamAll();

    /**
     * Obtiene todlos paises activos como un stream con filtros.
     *
     * @param filter Filtros a aplicar
     * @return Multi que emite cada moneda que cumple los filtros
     */
    Multi<CountryResponse> streamWithFilter(CountryFilter filter);

}
