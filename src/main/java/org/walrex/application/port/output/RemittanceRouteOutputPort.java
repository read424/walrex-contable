package org.walrex.application.port.output;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.ExchangeRateRouteInfo;
import org.walrex.domain.model.RemittanceRoute;

import java.util.List;

/**
 * Puerto de salida para consultar rutas de remesas configuradas
 */
public interface RemittanceRouteOutputPort {

    /**
     * Obtiene todas las rutas de remesas activas
     *
     * @return Uni con la lista de rutas configuradas
     * @deprecated Usar {@link #findAllActiveExchangeRateRoutes()} que incluye info del país destino
     */
    @Deprecated
    Uni<List<RemittanceRoute>> findAllActiveRoutes();

    /**
     * Obtiene todas las rutas activas con info completa para exchange rates,
     * incluyendo datos del país destino (countryToId, countryToCode).
     *
     * @return Uni con la lista de ExchangeRateRouteInfo
     */
    Uni<List<ExchangeRateRouteInfo>> findAllActiveExchangeRateRoutes();
}
