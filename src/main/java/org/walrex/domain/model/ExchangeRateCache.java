package org.walrex.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
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
     * Código ISO2 del país origen (ej: PE, EC, US).
     */
    private String countryFromCode;

    /**
     * Código ISO2 del país destino (ej: VE, PE).
     */
    private String countryToCode;

    /**
     * Timestamp de la última actualización.
     */
    private OffsetDateTime updatedAt;

    /**
     * Genera la clave única para Redis basada en país origen, moneda origen, país destino, moneda destino y fecha.
     *
     * Formato: exchange_rate:{countryFromCode}:{currencyFrom}:{countryToCode}:{currencyTo}:{date}
     *
     * Incluir los códigos de país permite identificar fácilmente a qué países pertenece cada key
     * (ej: PE:PEN:VE:VES). La fecha asegura un cache miss al inicio de cada nuevo día.
     *
     * @return Clave única para Redis
     */
    public String generateCacheKey() {
        return String.format("exchange_rate:%s:%s:%s:%s:%s",
                countryFromCode, currencyFrom, countryToCode, currencyTo,
                LocalDate.now());
    }

    /**
     * Genera una clave de caché estática.
     *
     * @param countryFromCode Código ISO2 del país origen (ej: PE, EC)
     * @param currencyFrom Código de moneda origen
     * @param countryToCode Código ISO2 del país destino (ej: VE)
     * @param currencyTo Código de moneda destino
     * @param date Fecha para la cual se genera la clave
     * @return Clave única para Redis
     */
    public static String generateCacheKey(
            String countryFromCode,
            String currencyFrom,
            String countryToCode,
            String currencyTo,
            LocalDate date) {
        return String.format("exchange_rate:%s:%s:%s:%s:%s",
                countryFromCode, currencyFrom, countryToCode, currencyTo, date);
    }
}
