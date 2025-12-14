package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.Country;

public interface CreateCountryUseCase {
    /**
     * Crea una nueva moneda en el sistema.
     *
     * @param country Datos necesarios para crear la moneda
     * @return Uni con la moneda creada
     * @throws com.walrex.domain.exception.DuplicateCountryException
     *         si ya existe una moneda con los mismos datos únicos
     * @throws com.walrex.domain.exception.InvalidCountryDataException
     *         si los datos no cumplen las reglas de negocio
     */
    Uni<Country> agregar(Country country);
}
