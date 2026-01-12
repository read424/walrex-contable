package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.application.dto.response.ProductBrandResponse;

import java.util.List;

/**
 * Puerto de entrada (caso de uso) para listar todas las marcas de producto sin paginación.
 *
 * Este caso de uso retorna todas las marcas activas optimizadas para:
 * - Componentes de selección (dropdowns, autocomplete)
 * - Cacheo de larga duración (15 minutos)
 * - Mejor rendimiento al evitar paginación
 *
 * Implementa cache-aside pattern con invalidación automática en create/update/delete.
 */
public interface ListAllProductBrandUseCase {

    /**
     * Obtiene todas las marcas de producto activas sin paginación.
     *
     * @return Uni con lista completa de marcas
     */
    Uni<List<ProductBrandResponse>> findAll();
}
