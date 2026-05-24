package org.walrex.infrastructure.adapter.inbound.websocket.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Mensaje entrante del WebSocket de Finnhub.
 *
 * Tipos conocidos:
 *   "trade" → data[] con precios
 *   "ping"  → keepalive (no requiere respuesta)
 *   "error" → mensaje de error en "msg"
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FinnhubMessage {

    private String type;

    private List<FinnhubTradeData> data;

    private String msg;
}
