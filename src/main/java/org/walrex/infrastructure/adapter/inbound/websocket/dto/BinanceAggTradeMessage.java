package org.walrex.infrastructure.adapter.inbound.websocket.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Mensaje aggTrade de Binance WebSocket.
 *
 * Ejemplo: wss://stream.binance.com:443/ws/btcusdt@aggTrade
 * {
 *   "e": "aggTrade",
 *   "E": 123456789,
 *   "s": "BTCUSDT",
 *   "p": "50000.00",
 *   "q": "0.001",
 *   "T": 123456785,
 *   "m": true
 * }
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BinanceAggTradeMessage {

    @JsonProperty("s")
    private String symbol;

    /** Precio del trade agregado (String en la API de Binance) */
    @JsonProperty("p")
    private String price;

    /** Timestamp del evento en epoch millis */
    @JsonProperty("E")
    private Long eventTime;
}
