package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.application.dto.query.ProductTemplateFilter;
import org.walrex.application.dto.response.ProductTemplateSelectResponse;

import java.util.List;

/**
 * Caso de uso para obtener todas las plantillas de producto sin paginación.
 *
 * Útil para componentes de selección (combos, selects) en el frontend.
 */
public interface ListAllProductTemplateUseCase {

    /**
     * Obtiene todas las plantillas de producto que cumplen el filtro sin paginación.
     *
     * Retorna un DTO optimizado para componentes de selección.
     * Implementa cache-aside pattern con TTL de 15 minutos.
     *
     * @param filter Filtros opcionales (por defecto solo plantillas activas)
     * @return Uni con lista completa de plantillas optimizadas
     */
    Uni<List<ProductTemplateSelectResponse>> findAll(ProductTemplateFilter filter);
}
