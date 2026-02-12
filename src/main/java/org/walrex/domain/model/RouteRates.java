package org.walrex.domain.model;

import java.util.List;

/**
 * Record para las tasas de un par de monedas específico
 */
public record RouteRates(
        Integer currencyFromId,        // ID moneda origen (ej: 1)
        String currencyFromCode,       // Código moneda origen (ej: PER)
        Integer currencyToId,          // ID moneda destino (ej: 2)
        String currencyToCode,         // Código moneda destino (ej: VEN)
        Long countryCurrencyFromId,    // ID country_currency origen
        Long countryCurrencyToId,      // ID country_currency destino
        String countryFromCode,        // Código ISO2 país origen (ej: PE, EC)
        String countryToCode,          // Código ISO2 país destino (ej: VE)
        List<ExchangeRate> buyRates,   // Tasas para comprar USDT con moneda origen
        List<ExchangeRate> sellRates   // Tasas para vender USDT por moneda destino
) {
}
