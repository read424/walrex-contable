package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.ExchangeCalculation;

import java.math.BigDecimal;

/**
 * Caso de uso para calcular tasas de cambio entre divisas.
 *
 * Realiza cálculos en tiempo real consultando Binance P2P API,
 * utilizando USDT como intermediario para conversiones cruzadas.
 */
public interface CalculateExchangeRateUseCase {
    /**
     * Calcula la conversión de divisas con tasa cruzada y margen aplicado.
     *
     * @param amount         Monto a convertir en la moneda base
     * @param baseCurrency   Código de la moneda base (origen)
     * @param quoteCurrency  Código de la moneda cotizada (destino)
     * @param margin         Margen de ganancia a aplicar (en porcentaje)
     * @return Uni con el cálculo de cambio completo
     * @throws org.walrex.domain.exception.RemittanceRouteNotFoundException
     *         si no existe una ruta configurada para el par de monedas
     * @throws org.walrex.domain.exception.ExchangeRateProviderException
     *         si hay un error al consultar la API de Binance
     */
    Uni<ExchangeCalculation> calculateExchangeRate(
            BigDecimal amount,
            String baseCurrency,
            String quoteCurrency,
            BigDecimal margin
    );
}
