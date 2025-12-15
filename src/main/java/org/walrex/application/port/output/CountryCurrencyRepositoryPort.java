package org.walrex.application.port.output;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.CountryCurrency;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida para operaciones de persistencia de CountryCurrency.
 */
public interface CountryCurrencyRepositoryPort {

    /**
     * Guarda una relación país-moneda.
     */
    Uni<CountryCurrency> save(CountryCurrency countryCurrency);

    /**
     * Encuentra todas las monedas de un país.
     */
    Uni<List<CountryCurrency>> findByCountryId(Integer countryId);

    /**
     * Encuentra la moneda predeterminada de un país.
     */
    Uni<Optional<CountryCurrency>> findPrimaryByCountryId(Integer countryId);

    /**
     * Encuentra una relación específica país-moneda.
     */
    Uni<Optional<CountryCurrency>> findByCountryIdAndCurrencyId(Integer countryId, Integer currencyId);

    /**
     * Verifica si existe una relación país-moneda.
     */
    Uni<Boolean> existsByCountryIdAndCurrencyId(Integer countryId, Integer currencyId);

    /**
     * Establece todas las monedas de un país como no predeterminadas.
     */
    Uni<Void> unsetAllPrimaryForCountry(Integer countryId);

    /**
     * Actualiza el estado operacional de una moneda en un país.
     */
    Uni<CountryCurrency> updateOperationalStatus(Integer countryId, Integer currencyId, Boolean isOperational);

    /**
     * Actualiza una relación país-moneda existente.
     */
    Uni<CountryCurrency> update(CountryCurrency countryCurrency);
}