package org.walrex.application.port.output;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.Country;
import org.walrex.domain.model.Currency;
import org.walrex.domain.model.ExchangeRateSource;
import org.walrex.domain.model.RemittanceExchangeRate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface RemittanceExchangeRateOutputPort {

    /**
     * Obtiene todos los países disponibles para remesas
     */
    Uni<List<Country>> findAllAvailableCountries();

    /**
     * Obtiene las monedas operativas de un país
     */
    Uni<List<Currency>> findOperationalCurrenciesByCountry(Integer countryId);

    /**
     * Obtiene todas las fuentes de tasa de cambio activas
     */
    Uni<List<ExchangeRateSource>> findAllActiveExchangeRateSources();

    /**
     * Obtiene las tasas más recientes para un par de monedas
     */
    Uni<List<RemittanceExchangeRate>> findLatestRatesByCurrencyPair(Long baseCurrencyId, Long quoteCurrencyId);

    /**
     * Obtiene todas las tasas disponibles desde una moneda base
     * Retorna un mapa: País -> Monedas -> Fuentes de tasa -> Tasa
     */
    Uni<Map<Country, Map<Currency, Map<ExchangeRateSource, BigDecimal>>>> findAllRatesFromBaseCurrency(Long baseCurrencyId);

    /**
     * Actualiza o inserta una nueva tasa
     */
    Uni<RemittanceExchangeRate> upsertExchangeRate(Long baseCurrencyId, Long quoteCurrencyId, 
                                                   Long exchangeRateSourceId, BigDecimal rate);

    /**
     * Obtiene un país por su código ISO2
     */
    Uni<Country> findCountryByIso2(String iso2);

    /**
     * Obtiene una moneda por su código ISO3
     */
    Uni<Currency> findCurrencyByCode(String code);

    /**
     * Obtiene una fuente de tasa por su código
     */
    Uni<ExchangeRateSource> findExchangeRateSourceByCode(String code);

    /**
     * Obtiene países destino con sus monedas para remesas
     */
    Uni<List<org.walrex.infrastructure.adapter.outbound.persistence.dto.RemittanceRouteResultDto>> findDestinationCountriesWithCurrencies(Integer countryId);

    /**
     * Obtiene los tipos de tasa de cambio activos para un país
     */
    Uni<List<org.walrex.domain.model.ExchangeRateType>> findActiveRateTypesByCountry(Integer countryId);

    /**
     * Obtiene un tipo de tasa específico por país y código
     */
    Uni<org.walrex.domain.model.ExchangeRateType> findRateTypeByCountryAndCode(Integer countryId, String rateCode);
}