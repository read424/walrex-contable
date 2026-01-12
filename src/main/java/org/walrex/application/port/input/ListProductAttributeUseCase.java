package org.walrex.application.port.input;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.walrex.application.dto.query.PageRequest;
import org.walrex.application.dto.query.ProductAttributeFilter;
import org.walrex.application.dto.response.PagedResponse;
import org.walrex.application.dto.response.ProductAttributeResponse;

/**
 * Puerto de entrada para listar atributos de producto con paginación.
 *
 * Siguiendo el patrón hexagonal, este puerto define el contrato
 * para el caso de uso de listado paginado de atributos.
 */
public interface ListProductAttributeUseCase {
    /**
     * Lista atributos con paginación y filtros.
     *
     * @param pageRequest Configuración de paginación (page, size, sort)
     * @param filter Filtros opcionales (search, name, displayType, active, etc.)
     * @return Uni con respuesta paginada de atributos
     */
    Uni<PagedResponse<ProductAttributeResponse>> execute(PageRequest pageRequest, ProductAttributeFilter filter);

    /**
     * Obtiene todos los atributos activos como un stream reactivo.
     *
     * @return Multi que emite cada atributo individualmente
     */
    Multi<ProductAttributeResponse> streamAll();

    /**
     * Obtiene atributos como stream aplicando filtros.
     *
     * @param filter Filtros a aplicar
     * @return Multi que emite cada atributo que cumple los filtros
     */
    Multi<ProductAttributeResponse> streamWithFilter(ProductAttributeFilter filter);
}
