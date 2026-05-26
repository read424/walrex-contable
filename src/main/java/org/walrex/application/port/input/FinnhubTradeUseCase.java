package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Puerto de entrada invocado por el adapter de Finnhub WebSocket cada vez
 * que llega una actualización de precio para un símbolo suscrito.
 *
 * La implementación evaluará si el cambio supera el umbral configurado
 * y, en ese caso, consultará AstroPay para registrar la tasa oficial.
 */
public interface FinnhubTradeUseCase {

    Uni<Void> onTrade(String symbol, BigDecimal price, Instant tradeTime);
}
