package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.Currency;

public interface CreateCurrencyUseCase {
    /**
     * Crea una nueva moneda en el sistema.
     *
     * @param currency Datos necesarios para crear la moneda
     * @return Uni con la moneda creada
     * @throws com.walrex.domain.exception.DuplicateCurrencyException
     *         si ya existe una moneda con los mismos datos únicos
     * @throws com.walrex.domain.exception.InvalidCurrencyDataException
     *         si los datos no cumplen las reglas de negocio
     */
    Uni<Currency> execute(Currency currency);
}
