package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.Country;

public interface UpdateCountryUseCase {

    /**
     * Actualiza un pais existente con nuevos datos.
     *
     * @param id Identificador del pais a actualizar
     * @param country Nuevos datos para el pais
     * @return Uni con el pais actualizada
     * @throws com.walrex.domain.exception.CountryNotFoundException
     *         si no existe un pais con el ID proporcionado
     * @throws com.walrex.domain.exception.DuplicateCountryException
     *         si los nuevos datos entran en conflicto con otro pais existente
     */
    Uni<Country> execute(Integer id, Country country);
}
