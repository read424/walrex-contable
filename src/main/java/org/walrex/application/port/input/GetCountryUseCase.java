package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.Country;

public interface GetCountryUseCase {
    /**
     * Obtiene un pais por su ID.
     *
     * @param id Identificador único del pais
     * @return Uni con el pais encontrada
     * @throws com.walrex.domain.exception.CountryNotFoundException
     *         si no existe un pais con el ID proporcionado
     */
    Uni<Country> findById(Integer id);

    /**
     * Obtiene un pais por su código alfabético ISO 3166.
     *
     * @param alphabeticCode Código de 3 letras (ej: ARG, CHL, COL)
     * @return Uni con el pais encontrado
     * @throws com.walrex.domain.exception.CountryNotFoundException
     *         si no existe un pais con el código proporcionado
     */
    Uni<Country> findByAlphabeticCode3(String alphabeticCode);

    /**
     * Obtiene un pais por su código numérico ISO 3166.
     *
     * @param numericCode Código de 3 dígitos (ej: 032, 152, 170)
     * @return Uni con el pais encontrada
     * @throws com.walrex.domain.exception.CountryNotFoundException
     *         si no existe un pais con el código proporcionado
     */
    Uni<Country> findByNumericCode(String numericCode);
}
