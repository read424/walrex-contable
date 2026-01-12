package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.application.dto.query.ProductAttributeFilter;
import org.walrex.application.dto.response.ProductAttributeSelectResponse;

import java.util.List;

/**
 * Puerto de entrada para listar todos los atributos de producto sin paginación.
 *
 * Siguiendo el patrón hexagonal, este puerto define el contrato
 * para el caso de uso de listado completo de atributos (optimizado para selects).
 */
public interface ListAllProductAttributeUseCase {
    /**
     * Obtiene todos los atributos que cumplen el filtro sin paginación.
     * Retorna un DTO optimizado para componentes de selección.
     *
     * @param filter Filtros opcionales (por defecto solo atributos activos)
     * @return Uni con lista completa de atributos optimizados
     */
    Uni<List<ProductAttributeSelectResponse>> findAll(ProductAttributeFilter filter);
}
