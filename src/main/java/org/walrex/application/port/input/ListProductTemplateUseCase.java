package org.walrex.application.port.input;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.walrex.application.dto.query.PageRequest;
import org.walrex.application.dto.query.ProductTemplateFilter;
import org.walrex.application.dto.response.PagedResponse;
import org.walrex.application.dto.response.ProductTemplateResponse;

/**
 * Caso de uso para listar plantillas de producto con paginación.
 */
public interface ListProductTemplateUseCase {

    /**
     * Lista plantillas de producto con paginación y filtros.
     *
     * Implementa cache-aside pattern para mejorar el rendimiento.
     *
     * @param pageRequest Configuración de paginación (page, size, sort)
     * @param filter Filtros opcionales
     * @return Uni con respuesta paginada de plantillas
     */
    Uni<PagedResponse<ProductTemplateResponse>> execute(PageRequest pageRequest, ProductTemplateFilter filter);

    /**
     * Obtiene todas las plantillas de producto activas como un stream reactivo.
     *
     * @return Multi que emite cada plantilla individualmente
     */
    Multi<ProductTemplateResponse> streamAll();

    /**
     * Obtiene todas las plantillas de producto que cumplen el filtro como un stream.
     *
     * @param filter Filtros a aplicar
     * @return Multi que emite cada plantilla que cumple los filtros
     */
    Multi<ProductTemplateResponse> streamWithFilter(ProductTemplateFilter filter);
}
