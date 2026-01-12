package org.walrex.application.port.input;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.walrex.application.dto.query.PageRequest;
import org.walrex.application.dto.query.ProductCategoryUomFilter;
import org.walrex.application.dto.response.PagedResponse;
import org.walrex.application.dto.response.ProductCategoryUomResponse;

/**
 * Puerto de entrada para listar categorías de unidades de medida.
 *
 * Soporta múltiples formas de obtener categorías:
 * - Con paginación (para tablas y grids)
 * - Streaming reactivo (para exportaciones y SSE)
 */
public interface ListProductCategoryUomUseCase {
    /**
     * Lista categorías con paginación y filtros.
     *
     * @param pageRequest Configuración de paginación (page, size, sort)
     * @param filter Filtros opcionales (search, code, name, etc.)
     * @return Uni con respuesta paginada
     */
    Uni<PagedResponse<ProductCategoryUomResponse>> execute(PageRequest pageRequest, ProductCategoryUomFilter filter);

    /**
     * Obtiene todas las categorías activas como un stream reactivo.
     *
     * Útil para:
     * - Exportaciones
     * - Procesamiento en streaming
     * - Server-Sent Events (SSE)
     *
     * @return Multi que emite cada categoría individualmente
     */
    Multi<ProductCategoryUomResponse> streamAll();

    /**
     * Obtiene todas las categorías como un stream con filtros.
     *
     * @param filter Filtros a aplicar
     * @return Multi que emite cada categoría que cumple los filtros
     */
    Multi<ProductCategoryUomResponse> streamWithFilter(ProductCategoryUomFilter filter);
}
