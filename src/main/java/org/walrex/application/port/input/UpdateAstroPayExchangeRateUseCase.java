package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.AstroPayRate;

public interface UpdateAstroPayExchangeRateUseCase {

    /**
     * Consulta todas las rutas ASTROPAY activas en BD, llama a AstroPay por cada par,
     * aplica margen y persiste en Redis (exchange_rate:*) + BD.
     */
    Uni<Void> updateRatesForActiveRoutes();

    /**
     * Refresca todas las rutas ASTROPAY que involucren la moneda indicada (from o to).
     * Llamado por FinnhubTradeService cuando detecta variación > umbral en el par USD/{currency}.
     */
    Uni<Void> updateRatesForCurrency(String currencyCode);

    /**
     * Persiste la tasa ya obtenida de AstroPay para un par específico.
     * Usado por FinnhubTradeService cuando detecta variación > umbral.
     */
    Uni<Void> saveRateForPair(String currencyFrom, String currencyTo, AstroPayRate rate);
}
