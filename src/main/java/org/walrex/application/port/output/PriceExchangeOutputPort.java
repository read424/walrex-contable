package org.walrex.application.port.output;

import io.smallrye.mutiny.Uni;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Puerto de salida para persistir tasas de cambio
 */
public interface PriceExchangeOutputPort {

    /**
     * Guarda una tasa de cambio promedio
     *
     * @param currencyBaseCode Código ISO3 de la moneda base (ej: USD)
     * @param currencyQuoteCode Código ISO3 de la moneda quote (ej: PEN, VES)
     * @param averagePrice Precio promedio calculado
     * @param exchangeDate Fecha de la tasa
     * @return Uni con el ID del registro creado
     */
    Uni<Integer> saveAverageRate(
            String currencyBaseCode,
            String currencyQuoteCode,
            BigDecimal averagePrice,
            LocalDate exchangeDate
    );

    /**
     * Actualiza una tasa existente o crea una nueva
     * Si existe un registro activo (status=1) para la fecha, moneda base y moneda quote,
     * lo desactiva (status=0) y crea uno nuevo con status=1
     *
     * @param currencyBaseId ID de la moneda base en la tabla currencies
     * @param currencyQuoteId ID de la moneda quote en la tabla currencies
     * @param averagePrice Precio promedio
     * @param exchangeDate Fecha
     * @return Uni con el ID del registro creado
     */
    Uni<Integer> upsertRate(
            Integer currencyBaseId,
            Integer currencyQuoteId,
            BigDecimal averagePrice,
            LocalDate exchangeDate
    );

    /**
     * Busca la tasa activa más reciente por códigos de país y moneda.
     *
     * @param fromCountryCode Código ISO2 del país origen (ej: PE)
     * @param fromCurrencyCode Código ISO3 de la moneda origen (ej: PEN)
     * @param toCountryCode Código ISO2 del país destino (ej: VE)
     * @param toCurrencyCode Código ISO3 de la moneda destino (ej: VES)
     * @return Uni con Optional del amount_price, vacío si no existe
     */
    Uni<Optional<BigDecimal>> findActiveRateByCountriesAndCurrencies(
            String fromCountryCode, String fromCurrencyCode,
            String toCountryCode, String toCurrencyCode);
}
