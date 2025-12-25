package org.walrex.application.port.input;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.walrex.application.dto.query.AccountingAccountFilter;
import org.walrex.application.dto.query.PageRequest;
import org.walrex.application.dto.response.AccountingAccountResponse;
import org.walrex.application.dto.response.AccountingAccountSelectResponse;
import org.walrex.application.dto.response.PagedResponse;

import java.util.List;

/**
 * Puerto de entrada para listar cuentas contables.
 *
 * Soporta múltiples formas de obtener cuentas:
 * - Con paginación (para tablas y grids)
 * - Sin paginación (para selectores y autocomplete)
 * - Streaming reactivo (para exportaciones y SSE)
 */
public interface ListAccountingAccountsUseCase {
    /**
     * Lista cuentas con paginación y filtros.
     *
     * @param pageRequest Configuración de paginación (page, size, sort)
     * @param filter Filtros opcionales (search, code, type, etc.)
     * @return Uni con respuesta paginada
     */
    Uni<PagedResponse<AccountingAccountResponse>> execute(PageRequest pageRequest, AccountingAccountFilter filter);

    /**
     * Obtiene todas las cuentas que cumplen el filtro sin paginación.
     * Retorna un DTO optimizado para componentes de selección (select, dropdown, autocomplete).
     *
     * Útil para:
     * - Cargar opciones en selectores
     * - Autocomplete
     * - Listas de opciones en formularios
     *
     * @param filter Filtros opcionales (por defecto solo cuentas activas)
     * @return Uni con lista completa de cuentas optimizadas
     */
    Uni<List<AccountingAccountSelectResponse>> findAll(AccountingAccountFilter filter);

    /**
     * Obtiene todas las cuentas activas como un stream reactivo.
     *
     * Útil para:
     * - Exportaciones
     * - Procesamiento en streaming
     * - Server-Sent Events (SSE)
     *
     * @return Multi que emite cada cuenta individualmente
     */
    Multi<AccountingAccountResponse> streamAll();

    /**
     * Obtiene todas las cuentas como un stream con filtros.
     *
     * @param filter Filtros a aplicar
     * @return Multi que emite cada cuenta que cumple los filtros
     */
    Multi<AccountingAccountResponse> streamWithFilter(AccountingAccountFilter filter);
}
