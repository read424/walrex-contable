package org.walrex.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Modelo de dominio para datos de tasa de cambio almacenados en caché.
 *
 * Representa la tasa de cambio cruzada almacenada en Redis para evitar
 * escrituras innecesarias en la base de datos cuando la variación es mínima.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeRateCache {
    /**
     * Tasa de cambio cruzada con margen aplicado.
     */
    private BigDecimal rate;

    /**
     * Código de la moneda origen (FROM).
     */
    private String currencyFrom;

    /**
     * Código de la moneda destino (TO).
     */
    private String currencyTo;

    /**
     * ID del country_currency origen.
     */
    private Long countryCurrencyFromId;

    /**
     * ID del country_currency destino.
     */
    private Long countryCurrencyToId;

    /**
     * Timestamp de la última actualización.
     */
    private OffsetDateTime updatedAt;

    /**
     * Genera la clave única para Redis basada en el par de monedas y rutas.
     *
     * Formato: exchange_rate:{currencyFrom}:{currencyTo}:{countryCurrencyFromId}:{countryCurrencyToId}
     *
     * @return Clave única para Redis
     */
    public String generateCacheKey() {
        return String.format("exchange_rate:%s:%s:%d:%d",
                currencyFrom, currencyTo, countryCurrencyFromId, countryCurrencyToId);
    }

    /**
     * Genera una clave de caché estática.
     *
     * @param currencyFrom Código de moneda origen
     * @param currencyTo Código de moneda destino
     * @param countryCurrencyFromId ID de country_currency origen
     * @param countryCurrencyToId ID de country_currency destino
     * @return Clave única para Redis
     */
    public static String generateCacheKey(
            String currencyFrom,
            String currencyTo,
            Long countryCurrencyFromId,
            Long countryCurrencyToId) {
        return String.format("exchange_rate:%s:%s:%d:%d",
                currencyFrom, currencyTo, countryCurrencyFromId, countryCurrencyToId);
    }
}
