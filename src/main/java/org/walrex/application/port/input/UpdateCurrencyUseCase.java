package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.Currency;

public interface UpdateCurrencyUseCase {

    /**
     * Actualiza una moneda existente con nuevos datos.
     *
     * @param id Identificador de la moneda a actualizar
     * @param currency Nuevos datos para la moneda
     * @return Uni con la moneda actualizada
     * @throws com.walrex.domain.exception.CurrencyNotFoundException
     *         si no existe una moneda con el ID proporcionado
     * @throws com.walrex.domain.exception.DuplicateCurrencyException
     *         si los nuevos datos entran en conflicto con otra moneda existente
     */
    Uni<Currency> execute(Integer id, Currency currency);
}
