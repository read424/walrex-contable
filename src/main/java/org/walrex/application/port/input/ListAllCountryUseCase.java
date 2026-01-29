package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.application.dto.response.CountryResponse;

import java.util.List;

/**
 * Puerto de entrada (caso de uso) para listar todos los países sin paginación.
 *
 * Este caso de uso retorna todos los países activos optimizados para:
 * - Componentes de selección (dropdowns, autocomplete)
 * - Cacheo de larga duración (12 horas)
 * - Mejor rendimiento al evitar paginación
 *
 * Implementa cache-aside pattern con invalidación automática en create/update/delete.
 */
public interface ListAllCountryUseCase {

    /**
     * Obtiene todos los países activos sin paginación.
     *
     * @return Uni con lista completa de países
     */
    Uni<List<CountryResponse>> findAll();
}
