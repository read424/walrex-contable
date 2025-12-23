package org.walrex.application.port.input;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.walrex.application.dto.query.CurrencyFilter;
import org.walrex.application.dto.query.PageRequest;
import org.walrex.application.dto.response.CurrencyResponse;
import org.walrex.application.dto.response.CurrencySelectResponse;
import org.walrex.application.dto.response.PagedResponse;

import java.util.List;

public interface ListCurrenciesUseCase {
    /**
     * Lista monedas con paginación y filtros.
     *
     * @param pageRequest Configuración de paginación (page, size, sort)
     * @param filter Filtros opcionales (search, alphabeticCode, etc.)
     * @return Uni con respuesta paginada
     */
    Uni<PagedResponse<CurrencyResponse>> execute(PageRequest pageRequest, CurrencyFilter filter);

    /**
     * Obtiene todas las monedas que cumplen el filtro sin paginación.
     * Retorna un DTO optimizado para componentes de selección (select, dropdown, autocomplete).
     *
     * Útil para:
     * - Cargar opciones en selectores
     * - Autocomplete
     * - Listas de opciones en formularios
     *
     * @param filter Filtros opcionales (por defecto solo monedas activas)
     * @return Uni con lista completa de monedas optimizadas
     */
    Uni<List<CurrencySelectResponse>> findAll(CurrencyFilter filter);

    /**
     * Obtiene todas las monedas activas como un stream reactivo.
     *
     * Útil para:
     * - Exportaciones
     * - Procesamiento en streaming
     * - Server-Sent Events (SSE)
     *
     * @return Multi que emite cada moneda individualmente
     */
    Multi<CurrencyResponse> streamAll();

    /**
     * Obtiene todas las monedas activas como un stream con filtros.
     *
     * @param filter Filtros a aplicar
     * @return Multi que emite cada moneda que cumple los filtros
     */
    Multi<CurrencyResponse> streamWithFilter(CurrencyFilter filter);
}
