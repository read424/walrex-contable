package org.walrex.application.port.output;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.AstroPayRate;

/**
 * Puerto de salida para consultar tasas de cambio oficiales en AstroPay.
 *
 * Se invoca cuando Finnhub detecta un cambio de precio superior al umbral
 * configurado en un par de monedas con rate_provider = 'ASTROPAY'.
 */
public interface AstroPayPort {

    /**
     * Consulta la tasa de cambio entre dos monedas.
     *
     * @param from código ISO 4217 origen (ej: "PEN") — se convierte a lowercase internamente
     * @param to   código ISO 4217 destino (ej: "EUR")
     * @return Uni con la tasa oficial de AstroPay, o fallo si el servicio no responde
     */
    Uni<AstroPayRate> getExchangeRate(String from, String to);
}
