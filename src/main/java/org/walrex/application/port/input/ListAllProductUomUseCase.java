package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.application.dto.query.ProductUomFilter;
import org.walrex.application.dto.response.ProductUomSelectResponse;

import java.util.List;

/**
 * Puerto de entrada para listar todas las unidades de medida de productos sin paginación.
 *
 * Siguiendo el patrón hexagonal, este puerto define el contrato
 * para el caso de uso de listado completo (útil para componentes de selección).
 */
public interface ListAllProductUomUseCase {
    /**
     * Obtiene todas las unidades de medida que cumplen el filtro sin paginación.
     * Retorna un DTO optimizado para componentes de selección.
     *
     * @param filter Filtros opcionales (por defecto solo unidades activas)
     * @return Uni con lista completa de unidades de medida optimizadas
     */
    Uni<List<ProductUomSelectResponse>> findAll(ProductUomFilter filter);
}
