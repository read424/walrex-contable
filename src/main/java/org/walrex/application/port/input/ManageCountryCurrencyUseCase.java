package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.application.dto.response.CountryCurrencyResponse;

import java.util.List;

/**
 * Puerto de entrada para gestionar las monedas de los países.
 */
public interface ManageCountryCurrencyUseCase {

    /**
     * Lista todas las monedas asignadas a un país.
     */
    Uni<List<CountryCurrencyResponse>> listCurrenciesByCountry(Integer countryId);

    /**
     * Asigna una moneda a un país.
     */
    Uni<CountryCurrencyResponse> assignCurrency(Integer countryId, Integer currencyId);

    /**
     * Establece una moneda como predeterminada para un país.
     */
    Uni<CountryCurrencyResponse> setDefaultCurrency(Integer countryId, Integer currencyId);

    /**
     * Activa o desactiva la operación con una moneda en un país.
     */
    Uni<CountryCurrencyResponse> updateOperationalStatus(Integer countryId, Integer currencyId, Boolean isOperational);

    /**
     * Obtiene la moneda predeterminada de un país.
     */
    Uni<CountryCurrencyResponse> getDefaultCurrency(Integer countryId);
}