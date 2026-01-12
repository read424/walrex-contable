package org.walrex.application.port.input;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.walrex.application.dto.query.PageRequest;
import org.walrex.application.dto.query.ProductUomFilter;
import org.walrex.application.dto.response.PagedResponse;
import org.walrex.application.dto.response.ProductUomResponse;

/**
 * Puerto de entrada para listar unidades de medida de productos con paginación.
 *
 * Siguiendo el patrón hexagonal, este puerto define el contrato
 * para el caso de uso de listado paginado de unidades de medida.
 */
public interface ListProductUomUseCase {
    /**
     * Lista unidades de medida con paginación y filtros opcionales.
     *
     * @param pageRequest Configuración de paginación (page, size, sort)
     * @param filter Filtros opcionales (search, code, name, categoryId, active, etc.)
     * @return Uni con respuesta paginada de unidades de medida
     */
    Uni<PagedResponse<ProductUomResponse>> execute(PageRequest pageRequest, ProductUomFilter filter);

    /**
     * Stream reactivo de todas las unidades de medida activas.
     *
     * @return Multi que emite cada unidad de medida individualmente
     */
    Multi<ProductUomResponse> streamAll();

    /**
     * Stream reactivo de unidades de medida con filtros.
     *
     * @param filter Filtros a aplicar
     * @return Multi que emite cada unidad que cumple los filtros
     */
    Multi<ProductUomResponse> streamWithFilter(ProductUomFilter filter);
}
