package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.application.dto.query.ProductCategoryUomFilter;
import org.walrex.application.dto.response.ProductCategoryUomSelectResponse;

import java.util.List;

/**
 * Puerto de entrada para listar todas las categorías de unidades de medida sin paginación.
 *
 * Útil para componentes de selección (select, dropdown, autocomplete).
 */
public interface ListAllProductCategoryUomUseCase {
    /**
     * Obtiene todas las categorías que cumplen el filtro sin paginación.
     * Retorna un DTO optimizado para componentes de selección.
     *
     * Útil para:
     * - Cargar opciones en selectores
     * - Autocomplete
     * - Listas de opciones en formularios
     *
     * @param filter Filtros opcionales (por defecto solo categorías activas)
     * @return Uni con lista completa de categorías optimizadas
     */
    Uni<List<ProductCategoryUomSelectResponse>> findAll(ProductCategoryUomFilter filter);
}
