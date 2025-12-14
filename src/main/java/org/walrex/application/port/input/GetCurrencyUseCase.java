package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.Currency;

public interface GetCurrencyUseCase {
    /**
     * Obtiene una moneda por su ID.
     *
     * @param id Identificador único de la moneda
     * @return Uni con la moneda encontrada
     * @throws com.walrex.domain.exception.CurrencyNotFoundException
     *         si no existe una moneda con el ID proporcionado
     */
    Uni<Currency> findById(Integer id);

    /**
     * Obtiene una moneda por su código alfabético ISO 4217.
     *
     * @param alphabeticCode Código de 3 letras (ej: USD, EUR, PEN)
     * @return Uni con la moneda encontrada
     * @throws com.walrex.domain.exception.CurrencyNotFoundException
     *         si no existe una moneda con el código proporcionado
     */
    Uni<Currency> findByAlphabeticCode(String alphabeticCode);

    /**
     * Obtiene una moneda por su código numérico ISO 4217.
     *
     * @param numericCode Código de 3 dígitos (ej: 840, 978, 604)
     * @return Uni con la moneda encontrada
     * @throws com.walrex.domain.exception.CurrencyNotFoundException
     *         si no existe una moneda con el código proporcionado
     */
    Uni<Currency> findByNumericCode(String numericCode);
}
